/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.rbware.github.androidcouchpotato.widget;

/**
 * A PresenterSelector is used to obtain a {@link Presenter} for a given Object.
 * Similar to {@link Presenter},  PresenterSelector is stateless.
 */
public abstract class PresenterSelector {
    /**
     * Returns a presenter for the given item.
     */
    public abstract Presenter getPresenter(Object item);

    /**
     * Returns an array of all possible presenters.  The returned array should
     * not be modified.
     */
    public Presenter[] getPresenters() {
        return null;
    }
}
