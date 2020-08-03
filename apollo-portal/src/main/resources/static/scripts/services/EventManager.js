appService.service('EventManager', [function () {

    /**
     * subscribe EventType with any object
     * @type {string}
     */
    var ALL_OBJECT = '*';

    var eventRegistry = {};

    /**
     *
     * @param eventType acquired. event type
     * @param context   optional. event execute context
     * @param objectId  optional. subscribe object id and empty value means subscribe event type with all object
     */
    function emit(eventType, context, objectId) {
        if (!eventType) {
            return;
        }

        if (!eventRegistry[eventType]) {
            return;
        }

        context = context || {};

        if (objectId != null && objectId != ALL_OBJECT) {
            emitEventToSubscribers(eventRegistry[eventType][objectId], context);
            emitEventToSubscribers(eventRegistry[eventType][ALL_OBJECT]);
        } else {
            //emit event to subscriber which subscribed all object
            emitEventToSubscribers(eventRegistry[eventType][ALL_OBJECT], context);
        }
    }

    function emitEventToSubscribers(subscribers, context) {
        if (subscribers) {
            subscribers.forEach(function (subscriber) {
                subscriber.callback(context);
            })
        }
    }

    /**
     *
     * @param eventType acquired. event type
     * @param callback  acquired. callback function when event emitted
     * @param objectId  optional. subscribe object id and empty value means subscribe event type with all object
     */
    function subscribe(eventType, callback, objectId) {
        if (!eventType || !callback) {
            return;
        }

        objectId = objectId || ALL_OBJECT;
        eventRegistry[eventType] = eventRegistry[eventType] || {};
        eventRegistry[eventType][objectId] = eventRegistry[eventType][objectId] || [];

        var subscriber = {
            id: Math.random() * Math.random(),
            callback: callback
        };
        eventRegistry[eventType][objectId].push(subscriber);

        return subscriber.id;
    }

    /**
     * 
     * @param eventType  acquired. event type
     * @param subscriberId acquired. subscriber id which get from event manager when subscribe
     * @param objectId optional.    subscribe object id and empty value means subscribe event type with all object
     */
    function unsubscribe(eventType, subscriberId, objectId) {
        if (!eventType || !subscriberId) {
            return;
        }

            objectId = objectId || ALL_OBJECT;

        if (eventRegistry[eventType] && eventRegistry[eventType][objectId]) {
            var subscribers = eventRegistry[eventType][objectId];

            subscribers.forEach(function (subscriber, index) {
                if (subscriber.id == subscriberId) {
                    subscribers.splice(index, 1);
                }
            })
        }
    }

    return {
        ALL_OBJECT: ALL_OBJECT,

        emit: emit,
        subscribe: subscribe,
        unsubscribe: unsubscribe,

        EventType: {
            REFRESH_NAMESPACE: 'refresh_namespace',
            REFRESH_RELEASE_HISTORY: 'refresh_release_history',
            PUBLISH_NAMESPACE: 'pre_public_namespace',
            MERGE_AND_PUBLISH_NAMESPACE: 'merge_and_publish_namespace',
            PRE_ROLLBACK_NAMESPACE: 'pre_rollback_namespace',
            ROLLBACK_NAMESPACE: 'rollback_namespace',
            EDIT_GRAY_RELEASE_RULES: 'edit_gray_release_rules',
            UPDATE_GRAY_RELEASE_RULES: 'update_gray_release_rules',
            PUBLISH_DENY: 'publish_deny',
            EMERGENCY_PUBLISH: 'emergency_publish',
            PRE_DELETE_NAMESPACE: 'pre_delete_namespace',
            DELETE_NAMESPACE: 'delete_namespace',
            DELETE_NAMESPACE_FAILED: 'delete_namespace_failed',
            CHANGE_ENV_CLUSTER: "change_env_cluster",
            SYNTAX_CHECK_TEXT_FAILED: "syntax_check_text_failed"
        }

    }
}]);
