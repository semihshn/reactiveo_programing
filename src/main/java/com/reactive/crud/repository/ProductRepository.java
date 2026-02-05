package com.reactive.crud.repository;

import com.reactive.crud.entity.Product;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;

@ApplicationScoped
public class ProductRepository {

    @Inject
    PgPool client;

    private static final String FIND_ALL_QUERY =
            "SELECT id, name, description, price, created_at, updated_at FROM products ORDER BY id";

    private static final String FIND_BY_ID_QUERY =
            "SELECT id, name, description, price, created_at, updated_at FROM products WHERE id = $1";

    private static final String INSERT_QUERY =
            "INSERT INTO products (name, description, price, created_at, updated_at) " +
            "VALUES ($1, $2, $3, $4, $5) RETURNING id, name, description, price, created_at, updated_at";

    private static final String UPDATE_QUERY =
            "UPDATE products SET name = $1, description = $2, price = $3, updated_at = $4 " +
            "WHERE id = $5 RETURNING id, name, description, price, created_at, updated_at";

    private static final String DELETE_QUERY =
            "DELETE FROM products WHERE id = $1";

    public Multi<Product> findAll() {
        return client.query(FIND_ALL_QUERY)
                .execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(this::toProduct);
    }

    public Uni<Product> findById(Long id) {
        return client.preparedQuery(FIND_BY_ID_QUERY)
                .execute(Tuple.of(id))
                .onItem().transform(rowSet -> {
                    if (rowSet.size() == 0) {
                        return null;
                    }
                    return toProduct(rowSet.iterator().next());
                });
    }

    public Uni<Product> create(Product product) {
        LocalDateTime now = LocalDateTime.now();
        return client.preparedQuery(INSERT_QUERY)
                .execute(Tuple.of(
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        now,
                        now
                ))
                .onItem().transform(rowSet -> toProduct(rowSet.iterator().next()));
    }

    public Uni<Product> update(Product product) {
        LocalDateTime now = LocalDateTime.now();
        return client.preparedQuery(UPDATE_QUERY)
                .execute(Tuple.of(
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        now,
                        product.getId()
                ))
                .onItem().transform(rowSet -> {
                    if (rowSet.size() == 0) {
                        return null;
                    }
                    return toProduct(rowSet.iterator().next());
                });
    }

    public Uni<Boolean> delete(Long id) {
        return client.preparedQuery(DELETE_QUERY)
                .execute(Tuple.of(id))
                .onItem().transform(rowSet -> rowSet.rowCount() > 0);
    }

    private Product toProduct(Row row) {
        return new Product(
                row.getLong("id"),
                row.getString("name"),
                row.getString("description"),
                row.getBigDecimal("price"),
                row.getLocalDateTime("created_at"),
                row.getLocalDateTime("updated_at")
        );
    }
}
