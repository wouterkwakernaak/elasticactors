package org.elasticsoftware.elasticactors.util.concurrent;

/**
 * @author Joost van de Wijgerd
 */
public interface WorkExecutor<S,T> {
    void execute(S shard,T work);
}