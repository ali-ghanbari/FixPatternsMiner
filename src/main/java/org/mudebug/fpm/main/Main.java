package org.mudebug.fpm.main;

import org.mudebug.fpm.commons.Crawler;

public class Main {

    public static void main(String[] args) {
        final Crawler c = new Crawler("/home/selab/New FixRuleMiner/FixRuleMiner/dataset/hdrepair-dataset");
        
//        System.out.println(c.ls().size());//
        c.ls().forEach(System.out::println);
    }

}
