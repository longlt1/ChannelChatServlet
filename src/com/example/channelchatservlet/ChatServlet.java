package com.example.channelchatservlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class ChatServlet extends HttpServlet {
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException {

		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		
		String message_type = req.getParameter("type");
		String user_name = user.getNickname();
		
		if( message_type.compareTo("get_token") == 0 ) {
			// generate and give token to user 
			String userId = user.getUserId();
			ChannelService channelService = ChannelServiceFactory.getChannelService();
			String token = channelService.createChannel(userId);
			addToCacheList( "token_list", userId );
			
			resp.setContentType("text/html");
			PrintWriter out = resp.getWriter();
			out.print( tokenMessage( user_name, token ) );
		}
		else {
			String message = req.getParameter("text");
			String chat_message = chatMessage( user_name, message );
			sendChannelMessageToAll( user_name, chat_message );
		}
	}

	private String chatMessage(String user_name, String message) {
		Map pack = new HashMap<String, String>();
		pack.put("type", "'message'");
		pack.put("user", "'" + user_name + "'");
		pack.put("text", "'" + message + "'");
		return responseToJson(pack);
	}

	private String tokenMessage(String user_name, String token) {
		Map pack = new HashMap<String, String>();
		pack.put("type", "'token'");
		pack.put("user", "'" + user_name + "'");
		pack.put("token", "'" + token + "'");
		return responseToJson(pack);
	}

	private void sendChannelMessageToAll( String author, String message ) {
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		List<String> keys = getUseridListFromCacahe( "token_list" );
		
		for( String k : keys ) {
			channelService.sendMessage(new ChannelMessage(k, message));
		}
	}
	
	public static String responseToJson( Map pack ) {
		String response = "{";
		if( pack != null ) {
			for( Object entry : pack.entrySet() ){
				response += ((Map.Entry)entry ).getKey() + ":" + ((Map.Entry)entry ).getValue() + ",";
			}

			// Remove trail comma
			response = response.substring( 0, response.length() - 1 );
			response += "}";
		}
		
		return response;
	}
	
	private static MemcacheService keycache = MemcacheServiceFactory.getMemcacheService();
	
	public static void addToCacheList( String key, String str ) {
		// Add to string list in cache 
		//
		if( !keycache.contains( key ) ) {
			List names = new LinkedList<String>();		
			names.add( str );
			keycache.put( key, names );
		}
		else {
			List names = (List )keycache.get( key );
			if( !names.contains( str ) ) {
				names.add(str);
				keycache.put( key, names );
			}
		}
	}
	
	private List<String> getUseridListFromCacahe( String key ) {
		return (List )keycache.get( key );	
	}
}
