package net.filebot.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import net.filebot.Cache;
import net.filebot.CacheType;

public enum ThumbnailProvider {

	TheTVDB, TheMovieDB;

	public URI getThumbnailURL(int id) {
		return URI.create("https://api.filebot.net/images/" + name().toLowerCase() + "/thumb/poster/" + id + ".png");
	}

	public byte[][] getThumbnails(int[] ids) throws Exception {
		CompletableFuture<HttpResponse<byte[]>>[] request = new CompletableFuture[ids.length];
		byte[][] response = new byte[ids.length][];

		// check cache
		for (int i = 0; i < request.length; i++) {
			response[i] = (byte[]) cache.get(ids[i]);
		}

		for (int i = 0; i < request.length; i++) {
			if (response[i] == null) {
				HttpRequest r = HttpRequest.newBuilder(getThumbnailURL(ids[i])).build();
				request[i] = http.sendAsync(r, BodyHandlers.ofByteArray());
			}
		}

		for (int i = 0; i < request.length; i++) {
			if (response[i] == null) {
				HttpResponse<byte[]> r = request[i].get();
				if (r.statusCode() == 200) {
					response[i] = r.body();
					cache.put(ids[i], response[i]);
				}
			}
		}

		return response;
	}

	// per instance cache
	private final Cache cache = Cache.getCache("thumbnail_" + ordinal(), CacheType.Persistent);

	// shared instance for all thumbnail requests
	private static final HttpClient http = HttpClient.newHttpClient();

}
