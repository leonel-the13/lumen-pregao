package ao.lumen.pregao.ranking.app;

import java.util.UUID;

final class RankingKeys {
    private RankingKeys() {}

    static String global() { return "rank:global"; }
    static String course(String code) { return "rank:course:" + code; }
    static String year(int y) { return "rank:year:" + y; }
    static String season(UUID id) { return "rank:season:" + id; }
    static String teams(UUID seasonId) { return "rank:teams:" + seasonId; }
    static String snapshot(String key) { return "snap:" + key; }
}
