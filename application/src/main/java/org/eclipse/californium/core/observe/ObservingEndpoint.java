package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Token;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservingEndpoint {

    /** The endpoint's address */
    private final InetSocketAddress address;

    /** The list of relations the endpoint has established with this server */
    private final List<ObserveRelation> relations;

    /**
     * Constructs a new ObservingEndpoint.
     *
     * @param address the endpoint's address
     * @throws NullPointerException if address is {@code null}.
     */
    public ObservingEndpoint(InetSocketAddress address) {
        if (address == null) {
            throw new NullPointerException("Address must not be null!");
        }
        this.address = address;
        this.relations = new CopyOnWriteArrayList<ObserveRelation>();
    }

    /**
     * Adds the specified observe relation.
     *
     * @param relation the relation
     */
    public void addObserveRelation(ObserveRelation relation) {
        relations.add(relation);
    }

    /**
     * Removes the specified observe relations.
     *
     * @param relation the relation
     */
    public void removeObserveRelation(ObserveRelation relation) {
        relations.remove(relation);
    }

    /**
     * Cancels all observe relations that this endpoint has established with
     * resources from this server.
     */
    public void cancelAll() {
        for (ObserveRelation relation : relations) {
            if (relation.isEstablished()) {
                relation.cancel();
            }
        }
    }

    /**
     * Returns the address of this endpoint-
     *
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Get observer relation for provided token.
     *
     * @param token observe request's token
     * @return observe relation, or {@code null}, if not available.
     * @deprecated obsolete
     */
    @Deprecated
    public ObserveRelation getObserveRelation(Token token) {
        if (token != null) {
            for (ObserveRelation relation : relations) {
                if (token.equals(relation.getExchange().getRequest().getToken())) {
                    return relation;
                }
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return relations.isEmpty();
    }
}
