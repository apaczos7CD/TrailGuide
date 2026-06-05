package pl.trailguide.backend.security;

final class MessageDigestUtils {

	private MessageDigestUtils() {
	}

	static boolean constantTimeEquals(String left, String right) {
		int result = 0;
		for (int i = 0; i < left.length(); i++) {
			result |= left.charAt(i) ^ right.charAt(i);
		}
		return result == 0;
	}
}
