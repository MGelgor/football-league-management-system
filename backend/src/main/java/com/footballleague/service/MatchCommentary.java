package com.footballleague.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.footballleague.entity.MatchEventType;

/**
 * Maç olayları için Türkçe yorum cümleleri. Aynı olay her seferinde aynı cümleyi alsın diye kalıp,
 * dakika + oyuncu adından seçilir. İsimlere gelen ekler (Kaya'nın, Ahmet'e) ünlü uyumuna göre üretilir.
 */
public final class MatchCommentary {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final String VOWELS = "aeıioöuü";
    private static final String BACK_VOWELS = "aıou";

    private MatchCommentary() {
    }

    /** team: oyuncunun takımı (kendi kalesine golde de). penalty: penaltı golü / kaçan penaltı. */
    public static String describe(MatchEventType type, int minute, String player, String assist, String team,
            boolean penalty) {
        return switch (type) {
            case GOAL -> penalty
                    ? pick(minute, player,
                            "GOL! " + player + " penaltıyı soğukkanlılıkla gole çevirdi.",
                            "GOL! Penaltı noktasına " + player + " geçti; kaleci bir yana, top bir yana!")
                    : assist != null
                    ? pick(minute, player,
                            "GOL! " + genitive(assist) + " pasında " + player + " topu ağlara gönderdi.",
                            "GOL! " + assist + " çizgiye indi, ortaladı; " + player + " son noktayı koydu.",
                            "GOL! " + player + ", " + genitive(assist) + " ara pasıyla kaleciyle karşı karşıya kaldı ve affetmedi.")
                    : pick(minute, player,
                            "GOL! " + player + " ceza sahası dışından müthiş bir vuruş yaptı, top ağlarda!",
                            "GOL! " + player + " bireysel çabasıyla rakip savunmayı geçti ve golü buldu.",
                            "GOL! Karambolde top " + dative(player) + " düştü, o da boş kaleye gönderdi.");
            case YELLOW_CARD -> pick(minute, player,
                    "Hakem sert müdahale sonrası " + dative(player) + " sarı kart gösterdi.",
                    player + " itirazları nedeniyle sarı kart gördü.",
                    player + " taktik faul yaptı ve sarı kartla cezalandırıldı.");
            case RED_CARD -> "Kırmızı kart! " + player + " oyundan atıldı, " + team + " eksik kaldı.";
            case INJURY -> pick(minute, player,
                    player + " yerde kaldı; sağlık ekibi oyuna devam edemeyeceğini işaret etti.",
                    player + " sakatlandı ve kenara geldi.");
            case OWN_GOAL -> pick(minute, player,
                    "Kendi kalesine gol! " + player + " topu uzaklaştırmak isterken kendi ağlarına gönderdi.",
                    "Talihsiz an: " + genitive(player) + " müdahalesi sonrası top " + team + " kalesine girdi.");
            case PENALTY_MISSED -> pick(minute, player,
                    "Penaltı kaçtı! " + genitive(player) + " vuruşunu kaleci köşesinde çıkardı.",
                    "Penaltı! " + player + " vurdu... kaleci doğru köşeye uzandı ve kurtardı!");
            case VAR_DISALLOWED -> pick(minute, player,
                    "VAR devrede: " + genitive(player) + " golü ofsayt gerekçesiyle iptal edildi.",
                    "Top ağlarda ama VAR incelemesinin ardından " + genitive(player) + " golü elle oynama nedeniyle geçersiz sayıldı.");
        };
    }

    public static String substitution(String incoming, String outgoing, String team) {
        return "Oyuncu değişikliği (" + team + "): " + incoming + " oyuna girdi, " + outgoing + " çıktı.";
    }

    /** "Kaya" → "Kaya'nın", "Ahmet" → "Ahmet'in": son kelimeye göre. */
    static String genitive(String name) {
        String vowel = harmony(name, "ı", "i", "u", "ü");
        return name + "'" + (endsWithVowel(name) ? "n" : "") + vowel + "n";
    }

    /** "Kaya" → "Kaya'ya", "Ahmet" → "Ahmet'e". */
    static String dative(String name) {
        String vowel = BACK_VOWELS.indexOf(lastVowel(name)) >= 0 ? "a" : "e";
        return name + "'" + (endsWithVowel(name) ? "y" : "") + vowel;
    }

    private static String harmony(String name, String a, String e, String o, String oe) {
        return switch (lastVowel(name)) {
            case 'a', 'ı' -> a;
            case 'o', 'u' -> o;
            case 'ö', 'ü' -> oe;
            default -> e;
        };
    }

    private static char lastVowel(String name) {
        String lower = name.toLowerCase(TURKISH);
        for (int i = lower.length() - 1; i >= 0; i--) {
            if (VOWELS.indexOf(lower.charAt(i)) >= 0) {
                return lower.charAt(i);
            }
        }
        return 'e';
    }

    private static boolean endsWithVowel(String name) {
        return !name.isEmpty() && VOWELS.indexOf(name.toLowerCase(TURKISH).charAt(name.length() - 1)) >= 0;
    }

    private static String pick(int minute, String player, String... variants) {
        return List.of(variants).get(Math.floorMod(Objects.hash(minute, player), variants.length));
    }
}
