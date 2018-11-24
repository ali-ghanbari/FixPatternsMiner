package edu.utdallas.fpm.pattern.handler.regexp;

import edu.utdallas.fpm.pattern.rules.Rule;

public interface AcceptanceState extends State {
    Rule getRule();
}
