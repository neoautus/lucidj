/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.lucidj.api;

// TODO: MOVE THIS TO Task WHERE IT BELONGS
public interface ComponentState
{
    // TODO: ARE THESE signal()s? SIGINT, SIGTERM...
    public static final int
        INIT = 0,
        DEPLOYING = 1,      // The component is waiting do be deployed alongside its dependencies
        DEPLOYED = 2,       // Deployed but not yet started
        ACTIVE = 3,         // Component active
        RUNNING = 4,        // Component active and running a task
        BLOCKED = 5,        // A thread that is blocked waiting for a monitor lock is in this state
        WAITING = 6,        // A thread that is waiting indefinitely for another thread to perform
                            // a particular action is in this state
        TERMINATED = 7,     // A thread that has exited is in this state
        ABORTED = 8,
        INTERRUPTED = 9,
        SIGTERM = 10,       // Testing the concept
        SIGSTART = 11;

    int getState ();
    boolean setState (int new_state);
    boolean signal (int signal);
    void addStateListener (ChangeListener listener);

    interface ChangeListener
    {
        void stateChanged (ComponentState ref);
    }
}

// EOF
