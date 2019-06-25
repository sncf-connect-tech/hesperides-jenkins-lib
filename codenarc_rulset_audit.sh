#!/bin/bash

set -o pipefail -o errexit -o nounset

cd "$( dirname "${BASH_SOURCE[0]}" )"

echo 'Disabled rules:'
comm -23 <(curl --fail --silent http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt | sed 's/ \+$//' | grep '    [A-Z]' | sort) \
         <(grep '    [A-Z]' codenarc_rules.groovy | sort) \
         | grep -Ev 'Grails|Javadoc'
