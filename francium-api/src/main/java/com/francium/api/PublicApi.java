package com.francium.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or member as part of Francium's public API.
 * <p>
 * Classes, methods, and fields annotated with {@code @PublicApi} are guaranteed to
 * remain stable across minor and patch versions. Backward-incompatible changes
 * to public API surfaces will only be introduced in major version bumps.
 * </p>
 * <p>
 * Types <strong>not</strong> annotated with {@code @PublicApi} are considered
 * internal implementation details. They may change, move, or be removed at any
 * time without notice. Consumers should avoid depending on them directly.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @PublicApi
 * public class FranciumLoader { ... }
 * }</pre>
 *
 * <h2>Stability guarantees</h2>
 * <ul>
 *   <li>Patch version: bug fixes only, no API changes</li>
 *   <li>Minor version: additions only, no removals or backward-incompatible changes</li>
 *   <li>Major version: arbitrary API changes permitted</li>
 * </ul>
 *
 * @since 2.2.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface PublicApi {
}
