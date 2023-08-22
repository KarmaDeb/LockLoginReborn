package eu.locklogin.api.module;

import ml.karmaconfigs.api.common.karma.source.KarmaSource;

/**
 * Source plugin
 */
public interface SourcePlugin extends KarmaSource {

    /**
     * Get source name
     *
     * @return the source name
     */
    @Override
    String name();
}
