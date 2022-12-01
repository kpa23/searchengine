package searchengine.services;

import lombok.Getter;

@Getter
public class SearchQueryBuilder {
    private final String query;
    private final String site;
    private final int limit;
    private final int offset;

    private SearchQueryBuilder(SearchBuilder builder) {
        site = builder.site;
        query = builder.query;
        limit = builder.limit;
        offset = builder.offset;
    }

    public static SearchBuilder newBuilder() {
        return new SearchBuilder();
    }

    public static final class SearchBuilder {

        private String query;
        private String site = null;
        private int limit = 20;
        private int offset = 0;

        private SearchBuilder() {
        }

        public SearchBuilder withQuery(String val) {
            if (val != null)
                query = val;
            return this;
        }

        public SearchBuilder withSite(String val) {
            if (val != null)
                site = val;
            return this;
        }

        public SearchBuilder withLimit(Integer val) {
            if (val != null)
                limit = val;
            return this;
        }

        public SearchBuilder withOffset(Integer val) {
            if (val != null)
                offset = val;
            return this;
        }

        public SearchQueryBuilder build() {
            return new SearchQueryBuilder(this);
        }

    }
}
