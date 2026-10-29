package app;

final class SearchCounts {
    private SearchCounts() {}
    static int literal(String source, String query, boolean matchCase) {
        if (query.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i <= source.length() - query.length();) {
            if (source.regionMatches(!matchCase, i, query, 0, query.length())) {
                count++;
                i += query.length();
            } else i++;
        }
        return count;
    }
}
