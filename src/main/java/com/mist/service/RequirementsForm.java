package com.mist.service;

import com.mist.model.SystemRequirements;

/**
 * A game's minimum system requirements as typed in a form.
 *
 * @param os        operating system
 * @param processor processor
 * @param memory    memory
 * @param graphics  graphics card
 * @param storage   disk space
 */
public record RequirementsForm(String os, String processor, String memory, String graphics, String storage) {

    /**
     * Builds the model value.
     *
     * @return the requirements
     * @throws IllegalArgumentException with a model key if a value is blank or too long
     */
    SystemRequirements toRequirements() {
        return new SystemRequirements(os, processor, memory, graphics, storage);
    }
}
