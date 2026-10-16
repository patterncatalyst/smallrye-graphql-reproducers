package org.acme;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class Api {

    /** A list of IDs as an argument: the schema prints {@code [ID]}, but every request fails. */
    @Query
    public List<String> echoIds(@Id List<String> ids) {
        return ids;
    }

    /** Control: a single ID argument works. */
    @Query
    public String echoId(@Id String id) {
        return id;
    }
}
