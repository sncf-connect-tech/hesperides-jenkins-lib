// This file is part of the hesperides-jenkins-lib distribution.
// (https://github.com/voyages-sncf-technologies/hesperides-jenkins-lib)
// Copyright (c) 2017 VSCT.
//
// hesperides-jenkins-lib is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as
// published by the Free Software Foundation, version 3.
//
// hesperides-jenkins-lib is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

ruleset {

    ruleset('rulesets/basic.xml') {
        EmptyCatchBlock(enabled:false)
    }

    ruleset('rulesets/braces.xml')

    ruleset('rulesets/concurrency.xml')

    ruleset('rulesets/convention.xml') {
        CompileStatic(enabled:false)
        MethodParameterTypeRequired(enabled:false)
        MethodReturnTypeRequired(enabled:false)
        NoDef(enabled:false)
        PublicMethodsBeforeNonPublicMethods(enabled:false)
        VariableTypeRequired(enabled:false)
    }

    ruleset('rulesets/design.xml') {
        Instanceof(enabled:false)
        NestedForLoop(enabled:false)
    }

    ruleset('rulesets/dry.xml') {
        DuplicateListLiteral(enabled:false)
        DuplicateNumberLiteral(enabled:false)
        DuplicateStringLiteral(enabled:false)
    }

    ruleset('rulesets/enhanced.xml')

    ruleset('rulesets/exceptions.xml') {
        ThrowRuntimeException(enabled:false)
    }

    ruleset('rulesets/formatting.xml') {
        ConsecutiveBlankLines (enabled:false)
        Indentation (enabled:false)
        LineLength (enabled:false)
        SpaceAfterOpeningBrace (enabled:false)
        SpaceAroundMapEntryColon (enabled:false)
        SpaceBeforeClosingBrace (enabled:false)
    }

    ruleset('rulesets/generic.xml')

    ruleset('rulesets/groovyism.xml')

    ruleset('rulesets/imports.xml') {
        NoWildcardImports(enabled:false)
        UnusedImport(enabled:false)
    }

    ruleset('rulesets/logging.xml') {
        SystemErrPrint(enabled:false)
        SystemOutPrint(enabled:false)
    }

    ruleset('rulesets/naming.xml') {
        FactoryMethodName(enabled:false)
    }

    ruleset('rulesets/security.xml') {
        JavaIoPackageAccess(enabled:false)
    }

    ruleset('rulesets/serialization.xml')

    ruleset('rulesets/size.xml') {
        AbcMetric(enabled:false)
        CrapMetric(enabled:false)
        CyclomaticComplexity(enabled:false)
        MethodCount(enabled:false)
    }

    ruleset('rulesets/unnecessary.xml'){
        UnnecessaryReturnKeyword(enabled:false)
        UnnecessarySetter(enabled:false)
    }

    ruleset('rulesets/unused.xml')

}
