package com.enomotosec.scanner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends ListenerAdapter {
    private static final String DISCORD_BOT_TOKEN = "MTUwODQ4Mjg0NjMzODE5MTUzMA.GMI_ty.XTa3NY47g2P0vDRliepF2vLENC9K8Bt51YmY_M";
    private static final String PREFIX = "!scan ";
    private static final Map<String, String> ENEMY_GROUPS = new HashMap<>();
    private static final Map<String, String> BLACKLISTED_FRIENDS = new HashMap<>();
    private static final Map<String, String> TRACKED_BADGES = new HashMap<>();
    private static final Map<String, String> TRACKED_GAMES = new HashMap<>();

    static {
        // Blacklisted Groups
        ENEMY_GROUPS.put("993480141", "山王ー団体 Sanno-Dantai");
        ENEMY_GROUPS.put("101878725", "横浜 | Yokohama");
        ENEMY_GROUPS.put("34065762", "[渋谷同盟] - Shibuya Doumei");
        ENEMY_GROUPS.put("565066633", "Devil's Covenant | 悪魔の契約");
        ENEMY_GROUPS.put("1051291555", "Chosen Devils | 選ばれた悪魔");
        ENEMY_GROUPS.put("14440827", "Sumiyoshi Rengō-kai 住吉会");
        ENEMY_GROUPS.put("53858089", "D양어니 | Yangeuni-Pa");
        ENEMY_GROUPS.put("14381841", "14K三合會");
        ENEMY_GROUPS.put("1030061361", "浜田組 | Hamada-gumi");
        ENEMY_GROUPS.put("13835630", "池田 一家 Ikeda-ikka");
        ENEMY_GROUPS.put("32024839", "廣雄 Kwong Hung");
        ENEMY_GROUPS.put("34805253", ".LEGION.");
        ENEMY_GROUPS.put("34107403", "El Azteca Mexican Restaurant");
        ENEMY_GROUPS.put("34202968", "AMC");
        ENEMY_GROUPS.put("35809619", "CONQUERED-INC.");
        ENEMY_GROUPS.put("35560914", "El Control.");
        ENEMY_GROUPS.put("14047973", "Henry Grasso Co., Inc.");
        ENEMY_GROUPS.put("14019766", "KILIHITO [斬人]");
        ENEMY_GROUPS.put("35788564", "Osaka Kurogane-kai 黒鉄会");
        ENEMY_GROUPS.put("35906074", "Ishinawa-Kai");
        ENEMY_GROUPS.put("317819165", "葉桜一家 | Hazakura-ikka");
        ENEMY_GROUPS.put("165751106", "Katagiri-ikka");
        ENEMY_GROUPS.put("78749178", "Kanagawa-ikka 神奈川一家");
        ENEMY_GROUPS.put("35783006", "西山 | Nishiyama-Ikka");
        ENEMY_GROUPS.put("813600163", "Hoshiguma Family | 星熊");
        ENEMY_GROUPS.put("1093181522", "Sayakawa Dynasty");
        ENEMY_GROUPS.put("35835805", "山岸家 | Yamagishi- Ka");
        ENEMY_GROUPS.put("35928243", "小野寺家 | Onodera-Ka");
        ENEMY_GROUPS.put("35844149", "茂樹家 | Shigeki-ka");
        ENEMY_GROUPS.put("35612673", "平山 | Hirayama Dynasty");
        ENEMY_GROUPS.put("35954100", "西山一花 | Nishiyama-Ikka funding group");
        ENEMY_GROUPS.put("53353863", "Shinryu Fight Club | 真龍格闘クラブ");
        //Blacklisted Users
        BLACKLISTED_FRIENDS.put("191531795", "MrSekiuchi");
        // Operational Games
        TRACKED_GAMES.put("15198987828", "[Remastered V1] San Junipero State Prison");
        TRACKED_GAMES.put("3039388345", "Shinjuku, 2006");
        TRACKED_GAMES.put("119438859200539", "Dragon Engine Hub");
        TRACKED_GAMES.put("16541340872", "Clark County");
        TRACKED_GAMES.put("9567991607", "Oakdale Federal Penitentiary");
        TRACKED_GAMES.put("128691057960014", "Pevek Correctional Colony");

    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        var jda = JDABuilder.createDefault(DISCORD_BOT_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(new Main())
                .setActivity(Activity.customStatus("Enomoto Advanced Recruit Scanner | !scan UserID"))
                .build();
        
        System.out.println("[Check] EnomotoSec Online[Stable Mode]");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        
        if (event.getAuthor().isBot() || !messageContent.startsWith(PREFIX)) {
            return;
        }

        String userId = messageContent.substring(PREFIX.length()).trim();

        if (!userId.matches("\\d+")) {
            event.getChannel().sendMessage("[X][Error]: Invalid userID.").queue();
            return;
        }

        event.getChannel().sendTyping().queue();

        try {
            var userUrl = "https://users.roblox.com/v1/users/" + userId;
            var userRequest = HttpRequest.newBuilder().uri(URI.create(userUrl)).GET().build();
            HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

            if (userResponse.statusCode() != 200) {
                event.getChannel().sendMessage("[X][Error]: User ID not found on Roblox.").queue();
                return;
            }

            var userData = new JSONObject(userResponse.body());
            var username = userData.getString("name");
            var displayName = userData.getString("displayName");
            var description = userData.optString("description", "None");
            var formattedJoinDate = ZonedDateTime.parse(userData.getString("created")).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

            boolean redDirectEnemyGroup = false;
            boolean redDirectBlacklistedFriend = false;
            boolean orangeFriendInEnemyGroup = false;
            boolean orangeTrackedGamePlayed = false;

            StringBuilder intelligenceReport = new StringBuilder();
            StringBuilder connectionAlerts = new StringBuilder();

            var groupUrl = "https://groups.roblox.com/v2/users/" + userId + "/groups/roles";
            var groupRequest = HttpRequest.newBuilder().uri(URI.create(groupUrl)).GET().build();
            HttpResponse<String> groupResponse = httpClient.send(groupRequest, HttpResponse.BodyHandlers.ofString());

            if (groupResponse.statusCode() == 200) {
                JSONArray groupsArray = new JSONObject(groupResponse.body()).getJSONArray("data");
                for (int i = 0; i < groupsArray.length(); i++) {
                    String currentGroupId = String.valueOf(groupsArray.getJSONObject(i).getJSONObject("group").getLong("id"));
                    if (ENEMY_GROUPS.containsKey(currentGroupId)) {
                        redDirectEnemyGroup = true;
                        String rankName = groupsArray.getJSONObject(i).getJSONObject("role").getString("name");
                        intelligenceReport.append("CRITICAL: Member of enemy group ").append(ENEMY_GROUPS.get(currentGroupId)).append(" [Rank: ").append(rankName).append("]\n");
                    }
                }
            }

            var friendsUrl = "https://friends.roblox.com/v1/users/" + userId + "/friends";
            var friendsRequest = HttpRequest.newBuilder().uri(URI.create(friendsUrl)).GET().build();
            HttpResponse<String> friendsResponse = httpClient.send(friendsRequest, HttpResponse.BodyHandlers.ofString());

            if (friendsResponse.statusCode() == 200) {
                JSONArray friendsArray = new JSONObject(friendsResponse.body()).getJSONArray("data");
                List<Long> friendIdsToScan = new ArrayList<>();
                Map<String, String> friendIdToNameMap = new HashMap<>();

                for (int i = 0; i < friendsArray.length(); i++) {
                    JSONObject friend = friendsArray.getJSONObject(i);
                    String fIdStr = String.valueOf(friend.getLong("id"));
                    String fName = friend.getString("name");

                    if (BLACKLISTED_FRIENDS.containsKey(fIdStr)) {
                        redDirectBlacklistedFriend = true;
                        intelligenceReport.append("THREAT: Direct friend with blacklisted user: ").append(BLACKLISTED_FRIENDS.get(fIdStr)).append(" (@").append(fName).append(")\n");
                        
                        friendIdsToScan.add(friend.getLong("id"));
                        friendIdToNameMap.put(fIdStr, fName);
                    }
                }

                if (!friendIdsToScan.isEmpty()) {
                    try {
                        JSONObject presenceRequestBody = new JSONObject();
                        presenceRequestBody.put("userIds", new JSONArray(friendIdsToScan));

                        var presenceUrl = "https://presence.roblox.com/v1/presence/users";
                        var presenceRequest = HttpRequest.newBuilder()
                                .uri(URI.create(presenceUrl))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(presenceRequestBody.toString()))
                                .build();

                        HttpResponse<String> presenceResponse = httpClient.send(presenceRequest, HttpResponse.BodyHandlers.ofString());

                        if (presenceResponse.statusCode() == 200) {
                            JSONObject presenceData = new JSONObject(presenceResponse.body());
                            if (presenceData.has("userPresences")) {
                                JSONArray presenceArray = presenceData.getJSONArray("userPresences");
                                for (int i = 0; i < presenceArray.length(); i++) {
                                    JSONObject presence = presenceArray.getJSONObject(i);
                                    String friendUserId = String.valueOf(presence.getLong("userId"));
                                    int userPresenceType = presence.getInt("userPresenceType");
                                    
                                    // userPresenceType: 0=Offline, 1=App, 2=InGame, 3=Studio
                                    if (userPresenceType == 2) {
                                        String friendName = friendIdToNameMap.getOrDefault(friendUserId, "Unknown");
                                        Object gameIdObj = presence.opt("gameId");
                                        
                                        if (gameIdObj != null && !gameIdObj.toString().equals("null")) {
                                            String gameIdStr = gameIdObj.toString();
                                            if (TRACKED_GAMES.containsKey(gameIdStr)) {
                                                orangeTrackedGamePlayed = true;
                                                connectionAlerts.append("ALERT: Monitored friend @").append(friendName)
                                                                .append(" is currently playing: ").append(TRACKED_GAMES.get(gameIdStr)).append("\n");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[!] Presence check skipped: " + e.getMessage());
                    }
                }
            } else {
                intelligenceReport.append("Friend list is private - unable to analyze network connections.\n");
            }

            Color embedColor;
            String statusTitle;

            if (redDirectEnemyGroup || redDirectBlacklistedFriend) {
                embedColor = new Color(192, 57, 43);
                statusTitle = "⚠ CRITICAL DIRECT RISK DETECTED";
            } else if (orangeFriendInEnemyGroup || orangeTrackedGamePlayed) {
                embedColor = new Color(230, 126, 34);
                statusTitle = "⚠ INTEL WARNING - SPY / LINK RISK";
            } else {
                embedColor = new Color(46, 204, 113);
                statusTitle = "✓ CLEARANCE APPROVED";
                intelligenceReport.append("Profile passes all background and connection verification tests.");
            }

            var embed = new EmbedBuilder()
                .setTitle(statusTitle)
                .setColor(embedColor)
                .addField("TARGET PROFILE", 
                    "Display Name: `" + displayName + "`\n" +
                    "Username: `" + username + "`\n" +
                    "User ID: `" + userId + "`\n" +
                    "Account Created: `" + formattedJoinDate + "`", 
                    false)
                .addField("DIRECT RISK ASSESSMENT", 
                    intelligenceReport.length() == 0 ? "No anomalies detected." : intelligenceReport.toString(), 
                    false);

            if (connectionAlerts.length() > 0) {
                embed.addField("NETWORK THREAT ANALYSIS", connectionAlerts.toString(), false);
            }

            if (!description.strip().isEmpty()) {
                embed.addField("USER DESCRIPTION", "```" + description + "```", false);
            }

            embed.setFooter("EnomotoSec Scan • " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC).format(Instant.now()));

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            System.err.println("Execution Error: " + e.getMessage());
            e.printStackTrace();
            event.getChannel().sendMessage("[!] [Error]: Processing failure during network loop analysis.").queue();
        }
    }
}
