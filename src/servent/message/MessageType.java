package servent.message;

public enum MessageType {
	NEW_NODE, WELCOME, SORRY, UPDATE, ADD, ADD_RESPONSE, PULL, PULL_RESPONSE, REMOVE, COMMIT, COMMIT_RESPONSE, REMOVE_RESPONSE, LEAVE_REQUEST, SUCC_LEAVING, LEAVE_GRANTED, PING, PONG, IS_REACHABLE, IS_REACHABLE_RESPONSE, QUESTION_EXISTENCE, QUESTION_EXISTENCE_RESPONSE, NEW_PREDECESSOR, NEW_PREDECESSOR_RESPONSE, REDUNDANT_COPY, BUSY, RELEASE_LOCK, REQUEST_LOCK, LOCK_GRANTED, POISON
}
