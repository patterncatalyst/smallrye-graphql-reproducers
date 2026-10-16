# SmallRye GraphQL reproducers

Minimal Quarkus apps that reproduce two SmallRye GraphQL server bugs. Each module holds one `@GraphQLApi`
class and a `@QuarkusTest` that asserts the **expected** behaviour, so the bug tests fail today and pass once
the bug is fixed. A control test in each module passes, which shows the app itself is wired correctly.

| Module | Bug | Issue |
|---|---|---|
| [`id-list-argument`](id-list-argument) | `@Id List<String>` prints `[ID]` in the schema, but every request fails with `Unknown Scalar Type [java.util.List]` | _link added after filing_ |
| [`deprecated-introspection`](deprecated-introspection) | `@io.smallrye.graphql.api.Deprecated(reason=…)` and `@java.lang.Deprecated` are printed as `@deprecated`, but introspection reports `isDeprecated: false` | _link added after filing_ |

## Run

Requires JDK 21+ and Maven 3.9+.

```console
# Quarkus 3.40.1 (SmallRye GraphQL 2.18.5, graphql-java 25.0)
mvn clean verify

# Quarkus 4.0.0.Beta1 (SmallRye GraphQL 3.0.0.Beta7, graphql-java 26.0)
mvn clean verify -Dquarkus.platform.version=4.0.0.Beta1

# Any SmallRye GraphQL build pinned over the BOM, e.g. a locally installed main
mvn clean verify -Dquarkus.platform.version=4.0.0.Beta1 -Dsmallrye-graphql.version=3.0.0.Beta-SNAPSHOT
```

Expected result on all three lines today:

```
id-list-argument          Tests run: 4, Failures: 2   (listOfIdsAsLiteral, listOfIdsAsVariable)
deprecated-introspection  Tests run: 5, Failures: 4   (every introspection test; the printed-schema test passes)
```

Reproduced on 2026-10-16 with `main` at `8a13b16` (three commits after `3.0.0.Beta8`), built with
`./mvnw install -DskipTests` for the server modules and pinned as above.

## `id-list-argument`

```java
@Query
public List<String> echoIds(@Id List<String> ids) { return ids; }
```

The schema export says `echoIds(ids: [ID]): [String]`, but both a literal and a variable are rejected:

```json
{"errors":[{"message":"argument 'ids' with value 'StringValue{value='a'}' is not a valid 'Unknown Scalar Type [java.util.List]'","locations":[{"line":1,"column":11}],"extensions":{"classification":"ValidationError"}}],"data":{"echoIds":null}}
```

The control, a single `@Id String`, returns `{"data":{"echoId":"a"}}`. SmallRye's own
[federation guide](https://github.com/smallrye/smallrye-graphql/blob/main/docs/federation.md) uses this pattern
(`product(@Id List<String> id)`).

## `deprecated-introspection`

With `quarkus.smallrye-graphql.schema-include-directives=true`, the printed schema says:

```graphql
withSmallRye: String @deprecated(reason : "Use `current`.")
oldItem: Item @deprecated(reason : "Use `item`.")
```

Introspection disagrees: `isDeprecated: false` and `deprecationReason: null` for `withSmallRye`, `withJavaLang`
and `oldItem`, and `fields` without `includeDeprecated` still lists the deprecated fields. graphql-java's
`GraphQLFieldDefinition.isDeprecated()` is `deprecationReason != null`, which only
`Builder.deprecate(reason)` sets; SmallRye attaches the annotation as a directive and never calls it.

## License

Apache License 2.0.
