package org.lsfa.bagpack.schedule;

import java.util.*;

/**
 */
public class main {
    static class Team {
        String teamName;
        List<String> players = new ArrayList<>();
        List<String> usedPlayers = new ArrayList<>();
        double playersForSession;
        int startIndex;

        Team(String... names) {
            players.addAll(Arrays.asList(names));
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        int calcTeamPlayersForSession(double playersPerSession, int totalPlayers) {
            double ratio = ((double)playersPerSession) / ((double)totalPlayers);
            this.playersForSession = ((double)players.size()) * ratio;

            return (int)this.playersForSession;
        }

        List<String> getPlayersForSession() {
            List<String> result = new ArrayList<>();

            while(result.size() < (int)playersForSession) {
                if (startIndex >= players.size())
                    startIndex = 0;
                result.add(players.get(startIndex++));
            }

            List<String> usedBefore = new ArrayList<>(result);

            usedBefore.retainAll(usedPlayers);

            for (String usedAlready : usedBefore)
                System.out.println(usedAlready);

            usedPlayers.addAll(result);
            return result;
        }

        List<String> getSpares() {
            List<String> spares = new ArrayList<>(players);
            spares.removeAll(usedPlayers);

            return spares;
        }
    }

    static class Session {
        Map<String, List<String>> teamPlayers = new HashMap<>();
    }

    public static void main(String[] args) {
        List<Team> teams = new ArrayList<>();
        int totalPlayers = 0;

        teams.add(new Team("CHARLIE EMERY",  "ALEX MACINTYRE",  "KYLE ASTALL",  "TOBY BARKER-SMITH",  "THOMAS BARONE",  "JOE BARRETT",  "GEORGE BROWN",  "ALEX CARROLL",  "CHARLIE EASTWOOD",  "BRUNO FERNANDES",  "DENELLE GODFREY",  "JAMES GREGORY",  "BEN HALE",  "ALEX HERNANDEZ",  "BARNEY HIRST",  "JAKE KILBRIDE",  "MAX LENIGHAN",  "JAMES MOORBY",  "CAMERON MOYLES",  "CLAUDE PAYNTER",  "BENJAMIN RATCLIFFE",  "HARRY VASEY",  "MICHAEL WALKER",  "BARNEY WARREN",  "JAMES WATERS",  "OLIVER WESTERMAN"));
        teams.get(0).setTeamName("U11");
        teams.add(new Team("ZACHARY ANGADI", "JAMIE BROADBENT", "JACK CLARKE", "LEO DIFFEY", "RUSSELL DJITIEU", "IAN KAMGA", "BEN LITTLEWOOD", "JAKE MYERS", "MAX PEARSON", "BRADLEY PRIDE", "OLIVER PRYSZCZYK", "CHARLIE TOKARSKI", "SAMUEL WELLS", "BRADLEY WOOD", "ALFIE WRIGHT"));
        teams.get(1).setTeamName("U12");
        teams.add(new Team("JACK WADE", "JACOB BURNLEY", "BILLY CARROLL", "JOSHUA CHAMBERS", "JAY CRUMBIE", "JACK DOLAN", "LEIGHTON EKE", "ETHAN HOOKS", "OWEN HOWE", "SAUL KANE", "JOE LITTLEWOOD", "ARCHIE MCDONNELL", "BILLY MURCHIE", "JOEL NETO", "HARRY NICOL", "ALFIE WILKINSON", "JAMES WILLIAMS DUSTAN"));
        teams.get(2).setTeamName("U13");

        for (Team team : teams)
            totalPlayers += team.players.size();

        int sessions = 5;
        double playersPerSession = ((double)totalPlayers) / ((double)sessions);
        int startIndex = 0;
        for (Team team : teams) {
            team.setStartIndex(startIndex);
            startIndex += team.calcTeamPlayersForSession(playersPerSession, totalPlayers);
        }

        List<Session> sessionList = new ArrayList<>();
        for (int s = 0; s < sessions; ++s) {
            Session session = new Session();
            for (Team team : teams) {
                session.teamPlayers.put(team.teamName, team.getPlayersForSession());
            }

            sessionList.add(session);
        }

        Map<String, List<String>> spareMap = new HashMap<>();
        for (Team team : teams) {
            List<String> spares = team.getSpares();

            if (!spares.isEmpty())
                spareMap.put(team.teamName, spares);
        }

        while (!spareMap.isEmpty()) {
            for (int sessionIndex = sessionList.size() - 1; sessionIndex >= 0 && !spareMap.isEmpty(); --sessionIndex) {
                Session session = sessionList.get(sessionIndex);
                Map.Entry<String, List<String>> entry = spareMap.entrySet().iterator().next();

                session.teamPlayers.get(entry.getKey()).add(entry.getValue().get(0));
                if (entry.getValue().size() > 1) {
                    entry.getValue().remove(0);
                } else
                    spareMap.remove(entry.getKey());
            }
        }

        String[] sessionTimes = {"09:00", "10:30", "12:00", "13:30", "15:00", "16:30"};
        for (int s=0; s < sessionList.size(); ++s) {
            int count = 0;
            Session session = sessionList.get(s);
            System.out.println("Session: "+sessionTimes[s]+" to "+sessionTimes[s+1]);
            for (Map.Entry<String, List<String>> tp : session.teamPlayers.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (String player : tp.getValue()) {
                    if (sb.length() > 0)
                        sb.append("; ");
                    sb.append(player);
                }
                count += tp.getValue().size();
                System.out.println("   "+tp.getKey()+" - "+sb.toString());
            }
            System.out.println("count: "+count);
        }
    }
}
