package org.elasticsearch.plugin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.profile.SearchProfileShardResults;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class DataMaskHandler extends BaseRestHandler {

    public static final String PLUGIN_NAME = "data_mask";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, "/DataMasking/{index}/_search"),
                new Route(POST, "/DataMask/{index}/_search"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient nodeClient) throws IOException {
        String clustername = nodeClient.settings().get("cluster.name");

        String indexNameInRequest = restRequest.param("index");
        SearchRequest searchRequest = new SearchRequest();
        IntConsumer setSize = (size) -> {
            searchRequest.source().size(size);
        };

        restRequest.withContentOrSourceParamParserOrNull((parser) -> {
            RestSearchAction.parseSearchRequest(searchRequest, restRequest, parser, setSize);
        });

        System.out.println("clustername: \t" + clustername);
        return channel -> {
            nodeClient.search(searchRequest, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    try {
                        XContentBuilder builder = channel.newBuilder();
                        newToXContent(searchResponse, builder, ToXContent.EMPTY_PARAMS, indexNameInRequest);

                        channel.sendResponse(
                                new BytesRestResponse(RestStatus.OK, builder));
                    } catch (Exception e) {
                        try {
                            XContentBuilder builder = channel.newBuilder();
                            builder.startObject();
                            builder.field("error", "there have an error!");
                            builder.endObject();
                            channel.sendResponse(
                                    new BytesRestResponse(RestStatus.OK, builder));
                        } catch (IOException e1) {
                            System.out.println("Failed to send failure response : " + e1);
                        }
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    System.out.printf("请求识别");
                }
            });
        };
    }


    private XContentBuilder newToXContent(SearchResponse searchResponse, XContentBuilder builder,
                                          ToXContent.Params params, String IndexName) throws IOException {
        builder.startObject();
        this.newInnerToXContent(searchResponse, builder, params, IndexName);
        builder.endObject();
        return builder;
    }

    private XContentBuilder newInnerToXContent(SearchResponse searchResponse, XContentBuilder builder, ToXContent.Params params, String IndexName) throws IOException {

        final ParseField SCROLL_ID = new ParseField("_scroll_id", new String[0]);
        final ParseField TOOK = new ParseField("took", new String[0]);
        final ParseField TIMED_OUT = new ParseField("timed_out", new String[0]);
        final ParseField TERMINATED_EARLY = new ParseField("terminated_early", new String[0]);
        final ParseField NUM_REDUCE_PHASES = new ParseField("num_reduce_phases", new String[0]);
        final SearchResponseSections internalResponse;
        final String scrollId;
        final int totalShards;
        final int successfulShards;
        final int skippedShards;
        final ShardSearchFailure[] shardFailures;
        final SearchResponse.Clusters clusters;
        final long tookInMillis;

        if (searchResponse.getScrollId() != null) {
            builder.field(SCROLL_ID.getPreferredName(), searchResponse.getScrollId());
        }

        builder.field(TOOK.getPreferredName(), searchResponse.getTook().millis());
        builder.field(TIMED_OUT.getPreferredName(), searchResponse.isTimedOut());
        if (searchResponse.isTerminatedEarly() != null) {
            builder.field(TERMINATED_EARLY.getPreferredName(), searchResponse.isTerminatedEarly());
        }

        if (searchResponse.getNumReducePhases() != 1) {
            builder.field(NUM_REDUCE_PHASES.getPreferredName(), searchResponse.getNumReducePhases());
        }

        RestActions.buildBroadcastShardsHeader(builder, params, searchResponse.getTotalShards(), searchResponse.getSuccessfulShards(), searchResponse.getSkippedShards(), searchResponse.getFailedShards(), searchResponse.getShardFailures());

        clusters = searchResponse.getClusters();
        clusters.toXContent(builder, params);
        internalResponse = new InternalSearchResponse(DataMasking(searchResponse.getHits()), (InternalAggregations) searchResponse.getAggregations(), searchResponse.getSuggest(), new SearchProfileShardResults(searchResponse.getProfileResults()), searchResponse.isTimedOut(), searchResponse.isTerminatedEarly(), searchResponse.getNumReducePhases());
        internalResponse.toXContent(builder, params);
        return builder;
    }


    private SearchHits DataMasking(SearchHits hits) {

        SearchHit[] searchHits = hits.getHits();
        SearchHit[] newSearchHits = new SearchHit[hits.getHits().length];
        int i = 0;

        for (SearchHit hit : searchHits) {

            JSONObject tempJson = JSON.parseObject(hit.getSourceAsString());

            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            for(String key:sourceAsMap.keySet()){
                try {
                    tempJson.put(key, FPE.encrypt(sourceAsMap.get(key).toString()));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
                builder.map((Map<String, ?>) tempJson);
                BytesReference bytesReference = BytesReference.bytes(builder);
                hit.sourceRef(bytesReference);
                newSearchHits[i++] = hit;
            } catch (IOException e) {
                System.out.println("Failed to change hit : " + e);
            }
        }
        return new SearchHits(searchHits, hits.getTotalHits(), hits.getMaxScore());
    }

}
