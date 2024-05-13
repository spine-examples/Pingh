package io.spine.internal.dependency

/**
 * Dependencies on Spine modules.
 */
@Suppress("ConstPropertyName")
public object Spine {

    /**
     * Versions for published Spine SDK artifacts.
     */
    private object ArtifactVersion {

        /**
         * The version of `core-java`.
         *
         * @see [Spine.CoreJava.server]
         * @see <a href="https://github.com/SpineEventEngine/core-java">core-java</a>
         */
        const val core = "1.9.0"
    }

    /**
     * Dependencies on `core-java` modules.
     *
     * See [`SpineEventEngine/core-java`](https://github.com/SpineEventEngine/core-java/).
     */
    public object CoreJava {
        private const val version = ArtifactVersion.core
        public const val server: String =
            "io.spine:spine-server:$version"
    }
}
