package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.footballleague.dto.TeamRequest;

/** Var olan takım adlarıyla çakışmayan, rastgele ad / kuruluş yılı / renklerle takım bilgisi üretir. */
@Component
public class RandomTeamGenerator {

    private static final List<String> CITIES = List.of(
            "Adana", "Ankara", "Antalya", "Bursa", "Konya", "Kayseri", "Samsun", "Sivas", "Rize", "Giresun",
            "Ordu", "Malatya", "Gaziantep", "Hatay", "Mersin", "Eskişehir", "Denizli", "Manisa", "Aydın", "Muğla",
            "Kocaeli", "Sakarya", "Bolu", "Düzce", "Zonguldak", "Kastamonu", "Sinop", "Amasya", "Tokat", "Çorum",
            "Erzurum", "Kars", "Van", "Elazığ", "Diyarbakır", "Şanlıurfa", "Kahramanmaraş", "Adıyaman", "Isparta",
            "Burdur", "Afyon", "Kütahya", "Uşak", "Balıkesir", "Çanakkale", "Edirne", "Tekirdağ", "Kırklareli",
            "Karabük", "Bartın", "Artvin", "Erzincan", "Niğde", "Aksaray", "Karaman", "Yozgat", "Kırşehir", "Bodrum");

    private static final List<String> SUFFIXES = List.of("spor", " FK", " Gücü", " Belediyespor", " İdmanyurdu", " SK");

    private static final List<String> COLORS = List.of(
            "Kırmızı", "Beyaz", "Mavi", "Lacivert", "Sarı", "Yeşil", "Siyah", "Bordo", "Turuncu", "Mor");

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final int MIN_FOUNDED_YEAR = 1900;
    private static final int MAX_FOUNDED_YEAR = 2020;

    public List<TeamRequest> generate(int count, Set<String> existingNames) {
        Set<String> taken = existingNames.stream().map(RandomTeamGenerator::normalize).collect(Collectors.toSet());

        record Candidate(String city, String name) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (String city : CITIES) {
            for (String suffix : SUFFIXES) {
                if (!taken.contains(normalize(city + suffix))) {
                    candidates.add(new Candidate(city, city + suffix));
                }
            }
        }
        if (candidates.size() < count) {
            throw new IllegalArgumentException("En fazla " + candidates.size() + " rastgele takım daha eklenebilir");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Collections.shuffle(candidates, random);
        // Önce her şehirden en fazla bir takım seçilir, yetmezse kalan adaylardan tamamlanır
        Set<String> usedCities = new HashSet<>();
        List<Candidate> picked = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (picked.size() < count && usedCities.add(candidate.city())) {
                picked.add(candidate);
            }
        }
        for (Candidate candidate : candidates) {
            if (picked.size() < count && !picked.contains(candidate)) {
                picked.add(candidate);
            }
        }

        return picked.stream()
                .map(candidate -> new TeamRequest(candidate.name(),
                        random.nextInt(MIN_FOUNDED_YEAR, MAX_FOUNDED_YEAR + 1), randomColors(random)))
                .toList();
    }

    private static String normalize(String name) {
        return name.toLowerCase(TURKISH);
    }

    private static String randomColors(ThreadLocalRandom random) {
        int first = random.nextInt(COLORS.size());
        int second = (first + 1 + random.nextInt(COLORS.size() - 1)) % COLORS.size();
        return COLORS.get(first) + "-" + COLORS.get(second);
    }
}
