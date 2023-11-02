package dev.vgerasimov.schema_migration.commercetools;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.vgerasimov.schema_migration.HistoryRecord;
import dev.vgerasimov.schema_migration.HistorySource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommerceToolsHistorySource implements HistorySource {

  private final ProjectApiRoot apiRoot;
  private final ObjectMapper objectMapper;
  private final String container;

  public CommerceToolsHistorySource(
      ProjectApiRoot apiRoot, ObjectMapper objectMapper, String container) {
    this.apiRoot = apiRoot;
    this.objectMapper = objectMapper;
    this.container = container;
  }

  @Override
  public List<HistoryRecord> getHistory() {
    GraphQLRequest request =
        GraphQLRequest.builder()
            .query(
                ""
                    + "query getZipcodeRules($containerName: String!, $limit: Int, $offset: Int) { "
                    + "    customObjects(container: $containerName, limit: $limit, offset: $offset) { "
                    + "        total "
                    + "        results { "
                    + "            container "
                    + "            key "
                    + "            id "
                    + "            value "
                    + "        } "
                    + "    } "
                    + "} ")
            .variables(
                builder ->
                    builder
                        .addValue("containerName", container)
                        .addValue("limit", 100)
                        .addValue("offset", 0))
            .build();
    CommonGraphResponse<CustomObjectPagedQueryResponse> response =
        foo(request, CustomObjectPagedQueryResponse.class);
    CustomObjectPagedQueryResponse data = response.getData();
    return data.getResults().stream()
        .map(
            co -> {
              try {
                return objectMapper.readValue(co.getValue().toString(), HistoryRecord.class);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public void saveRecord(HistoryRecord historyRecord) {}

  private <T> CommonGraphResponse<T> foo(GraphQLRequest req, Class<T> responseClass) {
    return apiRoot
        .graphql()
        .post(req)
        .executeBlocking(
            new TypeReference<CommonGraphResponse<T>>() {
              @Override
              public Type getType() {
                return TypeFactory.defaultInstance()
                    .constructParametricType(
                        CommonGraphResponse.class,
                        TypeFactory.defaultInstance()
                            .constructFromCanonical(responseClass.getCanonicalName()));
              }
            })
        .getBody();
  }

  public static class CommonGraphResponse<T> {
    private Map<String, T> data;
    private List<Object> errors;

    public T getData() {
      return data.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
    }
  }
}
