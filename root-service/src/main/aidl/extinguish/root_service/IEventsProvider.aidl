// IEventsProvider.aidl
package extinguish.root_service;

import extinguish.root_service.IEventsListener;

// Declare any non-default types here with import statements

interface IEventsProvider {
    boolean isRunning() = 0;
    void launch(String filter) = 1;
    void stop() = 2;
    oneway void registerListener(IEventsListener listener) = 3;
    oneway void unregisterListener(IEventsListener listener) = 4;
    String getForegroundPackage() = 5;

    void destroy() = 16777114;
}