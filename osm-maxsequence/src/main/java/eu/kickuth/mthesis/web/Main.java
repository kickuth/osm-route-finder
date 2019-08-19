package eu.kickuth.mthesis.web;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        staticFiles.location("/web");  // Static files
        staticFiles.expireTime(600); // cache static files for ten minutes
        get("/", (req, res) -> {res.redirect("/map.html");
            return null;
        });
    }
}
