package org.acme;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Type;

@GraphQLApi
public class Api {

    @Type("Item")
    public record Item(
            String current,
            @io.smallrye.graphql.api.Deprecated(reason = "Use `current`.") String withSmallRye,
            @java.lang.Deprecated String withJavaLang) {}

    @Query
    public Item item() {
        return new Item("a", "b", "c");
    }

    @Query
    @io.smallrye.graphql.api.Deprecated(reason = "Use `item`.")
    public Item oldItem() {
        return item();
    }
}
