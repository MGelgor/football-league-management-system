package com.footballleague.service;

import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.footballleague.dto.TeamRequest;
import com.footballleague.dto.TeamResponse;
import com.footballleague.entity.Player;
import com.footballleague.entity.Team;
import com.footballleague.exception.BigFourLockedException;
import com.footballleague.exception.DuplicateTeamNameException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.exception.TeamsLockedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    // Normal takımlar orta seviyeden başlar; 4 büyükler hep üst bantta (85-100)
    private static final int REGULAR_INITIAL_MIN_STRENGTH = 20;
    private static final int REGULAR_INITIAL_MAX_STRENGTH = 80;
    private static final int BIG_FOUR_INITIAL_MIN_STRENGTH = 88;
    private static final int BIG_FOUR_INITIAL_MAX_STRENGTH = 96;
    // Alt ligden çıkan takımlar ligin alt-orta seviyesinden başlar
    private static final int PROMOTED_MIN_STRENGTH = 35;
    private static final int PROMOTED_MAX_STRENGTH = 60;

    static final List<TeamRequest> BIG_FOUR = List.of(
            new TeamRequest("Galatasaray", 1905, "Sarı-Kırmızı"),
            new TeamRequest("Fenerbahçe", 1907, "Sarı-Lacivert"),
            new TeamRequest("Beşiktaş", 1903, "Siyah-Beyaz"),
            new TeamRequest("Trabzonspor", 1967, "Bordo-Mavi"));

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerRepository playerRepository;
    private final FileStorageService fileStorageService;
    private final SquadGenerator squadGenerator;
    private final RandomTeamGenerator randomTeamGenerator;

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findByActiveTrue().stream().map(TeamService::toResponse).toList();
    }

    /** Arşivlenmiş (küme düşmüş / silinmiş) takımlar da görüntülenebilir. */
    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long id) {
        return toResponse(teamRepository.findById(id).orElseThrow(() -> new TeamNotFoundException(id)));
    }

    public TeamResponse createTeam(TeamRequest request) {
        ensureTeamsEditable();
        validateFoundedYear(request.foundedYear());
        if (teamRepository.existsByNameIgnoreCaseAndActiveTrue(request.name())) {
            throw new DuplicateTeamNameException(request.name());
        }

        return toResponse(saveWithSquad(request, randomRegularStrength(), false));
    }

    /** Var olan adlarla çakışmayan, rastgele ad / yıl / renklerle count kadar takım (kadrolarıyla) ekler. */
    public List<TeamResponse> createRandomTeams(int count) {
        ensureTeamsEditable();
        Set<String> existingNames = teamRepository.findByActiveTrue().stream()
                .map(Team::getName)
                .collect(Collectors.toSet());

        return randomTeamGenerator.generate(count, existingNames).stream()
                .map(request -> saveWithSquad(request, randomRegularStrength(), false))
                .map(TeamService::toResponse)
                .toList();
    }

    /** Küme düşenlerin yerine lige yükselen rastgele takımlar (kadrolarıyla). Sezon kilidi uygulanmaz. */
    public List<Team> createPromotedTeams(int count) {
        if (count == 0) {
            return List.of();
        }
        Set<String> existingNames = teamRepository.findByActiveTrue().stream()
                .map(Team::getName)
                .collect(Collectors.toSet());
        return randomTeamGenerator.generate(count, existingNames).stream()
                .map(request -> saveWithSquad(request,
                        ThreadLocalRandom.current().nextInt(PROMOTED_MIN_STRENGTH, PROMOTED_MAX_STRENGTH + 1), false))
                .toList();
    }

    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team team = findTeamOrThrow(id);
        if (team.isBigFour()) {
            throw new BigFourLockedException(team.getName());
        }
        validateFoundedYear(request.foundedYear());

        teamRepository.findByNameIgnoreCaseAndActiveTrue(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateTeamNameException(request.name());
                });

        team.setName(request.name());
        team.setFoundedYear(request.foundedYear());
        team.setColors(request.colors());

        return toResponse(teamRepository.save(team));
    }

    /** Maç geçmişi olan takım (geçmiş sezon kayıtları bozulmasın diye) silinmez, arşivlenir. */
    public void deleteTeam(Long id) {
        Team team = findTeamOrThrow(id);
        if (team.isBigFour()) {
            throw new BigFourLockedException(team.getName());
        }
        ensureTeamsEditable();

        if (matchRepository.existsByHomeTeamIdOrAwayTeamId(id, id)) {
            team.setActive(false);
        } else {
            playerRepository.deleteByTeamId(id);
            teamRepository.delete(team);
        }
    }

    public TeamResponse updateLogo(Long id, MultipartFile file) {
        Team team = findTeamOrThrow(id);
        team.setLogoPath(fileStorageService.storeLogo(file));
        return toResponse(teamRepository.save(team));
    }

    /**
     * Uygulama açılışında çağrılır: 4 büyükler yoksa oluşturulur, aynı isimde normal takım varsa büyüğe çevrilir.
     * Kadrosu olmayan (bu özellikten önce oluşturulmuş) takımlara da kadro üretilir.
     */
    public void ensureBigFourAndSquads() {
        boolean seasonInProgress = seasonRepository.findTopByOrderBySeasonNumberDesc()
                .filter(season -> !season.isFinished())
                .isPresent();
        for (TeamRequest bigTeam : seasonInProgress ? List.<TeamRequest>of() : BIG_FOUR) {
            teamRepository.findByNameIgnoreCaseAndActiveTrue(bigTeam.name()).ifPresentOrElse(
                    existing -> {
                        existing.setBigFour(true);
                        existing.changeStrength(0);
                    },
                    () -> saveWithSquad(bigTeam, ThreadLocalRandom.current()
                            .nextInt(BIG_FOUR_INITIAL_MIN_STRENGTH, BIG_FOUR_INITIAL_MAX_STRENGTH + 1), true));
        }
        teamRepository.findByActiveTrue().stream()
                .filter(team -> !playerRepository.existsByTeamIdAndActiveTrue(team.getId()))
                .forEach(team -> playerRepository.saveAll(squadGenerator.generate(team)));
    }

    /** Kullanıcının eklediği oyuncular (varsa) kadroya girer, eksik mevkiler rastgele oyuncularla tamamlanır. */
    private Team saveWithSquad(TeamRequest request, int strength, boolean bigFour) {
        Team team = Team.builder()
                .name(request.name())
                .foundedYear(request.foundedYear())
                .colors(request.colors())
                .strength(strength)
                .seasonStartStrength(strength)
                .morale(Team.INITIAL_MORALE)
                .bigFour(bigFour)
                .build();
        // Forma numarası çakışması takım kaydedilmeden yakalansın
        List<Player> givenPlayers = request.players() == null ? List.of() : PlayerService.toPlayers(team, request.players());

        Team saved = teamRepository.save(team);
        playerRepository.saveAll(squadGenerator.complete(saved, givenPlayers));
        return saved;
    }

    private static int randomRegularStrength() {
        return ThreadLocalRandom.current().nextInt(REGULAR_INITIAL_MIN_STRENGTH, REGULAR_INITIAL_MAX_STRENGTH + 1);
    }

    private Team findTeamOrThrow(Long id) {
        return teamRepository.findById(id)
                .filter(Team::isActive)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    /** Takım listesi yalnızca devam eden bir sezon yokken değiştirilebilir. */
    private void ensureTeamsEditable() {
        seasonRepository.findTopByOrderBySeasonNumberDesc()
                .filter(season -> !season.isFinished())
                .ifPresent(season -> {
                    throw new TeamsLockedException();
                });
    }

    private void validateFoundedYear(Integer foundedYear) {
        int currentYear = Year.now().getValue();
        if (foundedYear > currentYear) {
            throw new IllegalArgumentException("Kuruluş yılı gelecekte olamaz");
        }
    }

    static String logoUrl(Team team) {
        return team.getLogoPath() != null ? "/uploads/" + team.getLogoPath() : null;
    }

    private static TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getFoundedYear(), team.getColors(), logoUrl(team),
                team.getStrength(), team.getMorale(), team.isBigFour(), team.getLastStrengthChange(),
                team.seasonStrengthChange(), team.isActive());
    }
}
