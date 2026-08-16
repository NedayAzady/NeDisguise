package me.nedayazady.database;

public class DisguiseData {
    private String name;
    private String rank;
    private String skin;

    public DisguiseData(String name, String rank, String skin) {
        this.name = name;
        this.rank = rank;
        this.skin = skin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }
}
