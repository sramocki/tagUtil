package org.tagUtil.util;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

public class Lookup {
    public static Trie<String, String> setupTrie() {
        Trie<String, String> trie = new PatriciaTrie<>();
        trie.put("mozart", "Wolfgang Amadeus Mozart");
        trie.put("lindberg", "Oskar Lindberg");
        trie.put("arban", "Jean-Baptiste Arban");
        trie.put("paganini", "Niccolò Paganini");
        trie.put("falla", "Manuel de Falla");
        trie.put("rachmaninov", "Sergei Rachmaninoff");
        trie.put("rachmaninoff", "Sergei Rachmaninoff");
        trie.put("debussy", "Claude Debussy");
        trie.put("piazzolla", "Astor Piazzolla");
        trie.put("tomasi", "Henri Tomasi");
        trie.put("hough", "Stephen Hough");
        trie.put("Dvorák", "Antonín Dvořák");
        trie.put("Dvořák", "Antonín Dvořák");
        return trie;
    }
}

