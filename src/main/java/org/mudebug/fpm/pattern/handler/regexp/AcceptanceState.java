package org.mudebug.fpm.pattern.handler.regexp;

import org.mudebug.fpm.pattern.rules.Rule;

public interface AcceptanceState extends State {
    Rule getRule();
}
