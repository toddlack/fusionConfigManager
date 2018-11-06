package com.sas.itq.search.configManager;

import com.sas.itq.search.configManager.connectors.Datasource;

/**
 * Enum of all supported fusion/solr entity types
 */
public enum EntityType {
    DATASOURCE() {
        @Override
        public Class classType() {
            return Datasource.class;
        }
    },
    INDEX_PIPELINE() {
        @Override
        public Class classType() {
            return IndexPipeline.class;
        }
    },
    QUERY_PIPELINE() {
        @Override
        public Class classType() {
            return QueryPipeline.class;
        }
    },
    SCHEDULE() {
        @Override
        public Class classType() {
            return Schedule.class;
        }
    },
    ROLE() {
        @Override
        public Class classType() {
            return Role.class;
        }
    },
    USER() {
        @Override
        public Class classType() {
            return User.class;
        }
    },
    COLLECTION() {
        @Override
        public Class classType() {
            return FusionCollection.class;
        }
    },
    COLLECTION_FEATURE() {
        @Override
        public Class classType() {
            return CollectionFeature.class;
        }
    },
    AGGREGATION() {
        @Override
        public Class classType() {
            return Aggregator.class;
        }
    },
    GROUP() {
      @Override
      public Class classType() {
          return FusionGroup.class;
      }
    },
    JOB() {
        @Override
        public Class classType() {
            return Job.class;
        }
    },
    SYSINFO() {
        @Override
        public Class classType() {
            return SystemInfo.class;
        }
    }, LINK() {
        @Override
        public Class classType() {
            return FusionLink.class;
        }
    }, OBJECT() {
        @Override
        public Class classType() {
            return FusionObject.class;
        }
    },
    QUERY_PROFILE() {
        @Override
        public Class classType() {
            return QueryProfile.class;
        }
    },
    INDEX_PROFILE() {
        @Override
        public Class classType() {
            return IndexProfile.class;
        }
    },
    PARSER() {
        @Override
        public Class classType() {
            return Parser.class;
        }
    };


    public abstract Class classType();

};
