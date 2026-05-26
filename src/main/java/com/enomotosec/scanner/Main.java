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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends ListenerAdapter {

    private static final String DISCORD_BOT_TOKEN = "//YOUR_DISCORD_BOT_TOKEN_HERE";
    private static final String PREFIX = "!scan ";
    private static final Map<String, String> ENEMY_GROUPS = new HashMap<>();
    private static final Map<String, String> BLACKLISTED_FRIENDS = new HashMap<>();
    private static final Map<String, String> TRACKED_BADGES = new HashMap<>();

    static {
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
        BLACKLISTED_FRIENDS.put("191531795", "MrSekiuchi");
        TRACKED_BADGES.put("1", "BadgeNameExample");
        ENEMY_GROUPS.put("250364003", "Miyazaki Kasumi - kai 《霞 - 甲斐》");
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
            event.getChannel().sendMessage("[X][EnomotoSec Error]: Invalid numerical ID format.").queue();
            return;
        }

        event.getChannel().sendTyping().queue();

        try {
            var userUrl = "https://users.roblox.com/v1/users/" + userId;
            var userRequest = HttpRequest.newBuilder().uri(URI.create(userUrl)).GET().build();
            HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

            if (userResponse.statusCode() != 200) {
                event.getChannel().sendMessage("[X][EnomotoSec Error]: User ID not found on Roblox.").queue();
                return;
            }

            var userData = new JSONObject(userResponse.body());
            var username = userData.getString("name");
            var displayName = userData.getString("displayName");
            var description = userData.optString("description", "None");
            var formattedJoinDate = ZonedDateTime.parse(userData.getString("created")).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

            boolean redDirectEnemyGroup = false;
            boolean redDirectBlacklistedFriend = false;
            boolean orangeBadgeDetected = false;
            boolean orangeFriendInEnemyGroup = false;

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
                        intelligenceReport.append("**Direct Threat:** Inside enemy group *").append(ENEMY_GROUPS.get(currentGroupId)).append("* as `").append(rankName).append("`.\n");
                    }
                }
            }

            var friendsUrl = "https://friends.roblox.com/v1/users/" + userId + "/friends";
            var friendsRequest = HttpRequest.newBuilder().uri(URI.create(friendsUrl)).GET().build();
            HttpResponse<String> friendsResponse = httpClient.send(friendsRequest, HttpResponse.BodyHandlers.ofString());

            if (friendsResponse.statusCode() == 200) {
                JSONArray friendsArray = new JSONObject(friendsResponse.body()).getJSONArray("data");
                int scanLimit = Math.min(friendsArray.length(), 90);
                List<Long> friendIdsToScan = new ArrayList<>();
                Map<String, String> friendIdToNameMap = new HashMap<>();

                for (int i = 0; i < scanLimit; i++) {
                    JSONObject friend = friendsArray.getJSONObject(i);
                    String fIdStr = String.valueOf(friend.getLong("id"));
                    String fName = friend.getString("name");

                    friendIdsToScan.add(friend.getLong("id"));
                    friendIdToNameMap.put(fIdStr, fName);

                    if (BLACKLISTED_FRIENDS.containsKey(fIdStr)) {
                        redDirectBlacklistedFriend = true;
                        intelligenceReport.append("[!!] **Dangerous Association:** Direct friends with blacklisted user: *").append(BLACKLISTED_FRIENDS.get(fIdStr)).append("* (@").append(fName).append(").\n");
                    }
                }

                if (!friendIdsToScan.isEmpty()) {
                    JSONObject batchRequestBody = new JSONObject();
                    batchRequestBody.put("userIds", new JSONArray(friendIdsToScan));

                    var batchUrl = "https://groups.roblox.com/v2/users/group-memberships";
                    var batchRequest = HttpRequest.newBuilder()
                            .uri(URI.create(batchUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(batchRequestBody.toString()))
                            .build();

                    HttpResponse<String> batchResponse = httpClient.send(batchRequest, HttpResponse.BodyHandlers.ofString());

                    if (batchResponse.statusCode() == 200) {
                        JSONArray membershipsArray = new JSONObject(batchResponse.body()).getJSONArray("data");
                        for (int i = 0; i < membershipsArray.length(); i++) {
                            JSONObject membership = membershipsArray.getJSONObject(i);
                            String currentGroupId = String.valueOf(membership.getJSONObject("group").getLong("id"));
                            
                            if (ENEMY_GROUPS.containsKey(currentGroupId)) {
                                orangeFriendInEnemyGroup = true;
                                String linkedUserStrId = String.valueOf(membership.getJSONObject("user").getLong("id"));
                                String linkedName = friendIdToNameMap.getOrDefault(linkedUserStrId, "Unknown Context");
                                
                                connectionAlerts.append("[!]**Spy Link:** Friend `@").append(linkedName)
                                                .append("` is a member of enemy group: *").append(ENEMY_GROUPS.get(currentGroupId)).append("*\n");
                            }
                        }
                    } else {
                        System.out.println("[X] Batch Endpoint Rejected: Code " + batchResponse.statusCode());
                    }
                }

                if (friendsArray.length() > 90) {
                    connectionAlerts.append("[!] *Note: Profile has ").append(friendsArray.length()).append("+ friends. First 90 User Scanned.*\n");
                }
            } else {
                intelligenceReport.append("[!] *Friend list is private; unable to parse spy networks.*\n");
            }

            var badgeUrl = "https://badges.roblox.com/v1/users/" + userId + "/badges?limit=50&sortOrder=Desc";
            var badgeRequest = HttpRequest.newBuilder().uri(URI.create(badgeUrl)).GET().build();
            HttpResponse<String> badgeResponse = httpClient.send(badgeRequest, HttpResponse.BodyHandlers.ofString());

            if (badgeResponse.statusCode() == 200) {
                JSONArray badgeArray = new JSONObject(badgeResponse.body()).getJSONArray("data");
                for (int i = 0; i < badgeArray.length(); i++) {
                    String currentBadgeId = String.valueOf(badgeArray.getJSONObject(i).getLong("id"));
                    if (TRACKED_BADGES.containsKey(currentBadgeId)) {
                        orangeBadgeDetected = true;
                        intelligenceReport.append("🔸 **Monitored Badge Owned:** ").append(TRACKED_BADGES.get(currentBadgeId)).append("\n");
                    }
                }
            }

            Color embedColor;
            String statusTitle;

            if (redDirectEnemyGroup || redDirectBlacklistedFriend) {
                embedColor = new Color(192, 57, 43);
                statusTitle = "[!!] CRITICAL DIRECT RISK DETECTED";
            } else if (orangeFriendInEnemyGroup || orangeBadgeDetected) {
                embedColor = new Color(230, 126, 34);
                statusTitle = "[!] INTEL WARNING: SPY / LINK RISK";
            } else {
                embedColor = new Color(46, 204, 113);
                statusTitle = "[✔] CLEARANCE APPROVED";
                intelligenceReport.append("[✔] Profile passes all background, badge, and connection verification tests safely.");
            }

            var embed = new EmbedBuilder()
                .setTitle(statusTitle + " : " + displayName)
                .setColor(embedColor)
                .addField("Identity", "User: `" + username + "`\nID: `" + userId + "`", true)
                .addField("Account Age", "Joined: " + formattedJoinDate, true)
                .addField("Direct Profile Dossier", intelligenceReport.length() == 0 ? "No anomalies." : intelligenceReport.toString(), false);

            if (connectionAlerts.length() > 0) {
                embed.addField("Network Connections Analysis", connectionAlerts.toString(), false);
            }

            if (!description.strip().isEmpty()) {
                embed.addField("Description", "```\n" + description + "\n```", false);
            }

            embed.setFooter("EnomotoSec Batch Link Matrix • " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC).format(Instant.now()));

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            System.err.println("Execution Error: " + e.getMessage());
            e.printStackTrace();
            event.getChannel().sendMessage("[!] [EnomotoSec Error]: Processing failure during network loop analysis.").queue();
        }
    }
}