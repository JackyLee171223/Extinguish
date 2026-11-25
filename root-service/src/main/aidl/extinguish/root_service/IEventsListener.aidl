// IEventsListener.aidl
package extinguish.root_service;

// Declare any non-default types here with import statements
import extinguish.ipc.result.EventResult;

interface IEventsListener {
    void onEvent(in EventResult event);
}