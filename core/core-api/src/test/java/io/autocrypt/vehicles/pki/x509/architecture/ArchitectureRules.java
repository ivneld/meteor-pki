package io.autocrypt.vehicles.pki.x509.architecture;

import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * docs/ARCHITECTURE.md 의 구조 규칙(R-xx). 규칙 ID 와 필드 이름이 대응한다.
 *
 * <p>
 * 패키지: {@code io.autocrypt.vehicles.pki.x509.{domain|usecase|api}} (core-api),
 * {@code io.autocrypt.vehicles.pki.x509.enums} (core-enum),
 * {@code io.autocrypt.vehicles.pki.x509.storage} (storage/db-core),
 * {@code io.autocrypt.vehicles.pki.x509.client.*} (clients/*),
 * {@code io.autocrypt.vehicles.pki.x509.support.*} (공통). 모든 모듈이 테스트 클래스패스에 있으므로
 * io.autocrypt.vehicles.pki.x509 전체를 한 번에 검사한다.
 *
 * <p>
 * domain 의 클래스는 둘로 나뉜다. Spring 빈인 <b>도메인 서비스</b>({@code *Reader}, {@code *Writer})와
 * <b>저장소 어댑터</b>({@code *Repository}), 그리고 순수한 <b>도메인 모델</b>(애그리거트, 값 객체 record,
 * {@code *Policy}).
 */
@AnalyzeClasses(packages = "io.autocrypt.vehicles.pki.x509", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRules {

    private static final String DOMAIN = "io.autocrypt.vehicles.pki.x509.domain..";

    private static final String USECASE = "io.autocrypt.vehicles.pki.x509.usecase..";

    private static final String API = "io.autocrypt.vehicles.pki.x509.api..";

    private static final String API_REQUEST = "io.autocrypt.vehicles.pki.x509.api.request..";

    private static final String API_RESPONSE = "io.autocrypt.vehicles.pki.x509.api.response..";

    private static final String STORAGE = "io.autocrypt.vehicles.pki.x509.storage..";

    private static final String CLIENT = "io.autocrypt.vehicles.pki.x509.client..";

    private static final String ENUMS = "io.autocrypt.vehicles.pki.x509.enums..";

    private static final String SUPPORT = "io.autocrypt.vehicles.pki.x509.support..";

    private static final String SUPPORT_STORAGE = "io.autocrypt.vehicles.pki.x509.support.storage..";

    private static final String ERROR = "io.autocrypt.vehicles.pki.x509.support.error..";

    // ----- R-01 도메인 모델은 순수하다 -----

    @ArchTest
    static final ArchRule R01_domain_model_is_pure = noClasses().that(domainModel())
        .should()
        .dependOnClassesThat(
                resideInAnyPackage("org.springframework..", "jakarta.persistence..", STORAGE, CLIENT, USECASE, API)
                    .or(domainBeans()))
        .as("R-01 도메인 모델(애그리거트, 값 객체, *Policy)은 Spring, JPA, storage, client, usecase, api, *Repository, *Reader, *Writer 를 모른다");

    @ArchTest
    static final ArchRule R01_domain_knows_no_outer_layers = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(USECASE, API, CLIENT)
        .as("R-01 domain 은 usecase, api, client 를 모른다. 외부 연동과 여러 도메인을 엮는 흐름은 usecase 가 한다");

    @ArchTest
    static final ArchRule R01_domain_uses_only_error_vocabulary_from_support = noClasses().that()
        .resideInAPackage(DOMAIN)
        .should()
        .dependOnClassesThat(resideInAPackage(SUPPORT).and(DescribedPredicate.not(resideInAPackage(ERROR))))
        .as("R-01 domain 이 support 에서 쓰는 것은 에러 어휘(support.error)뿐이다");

    @ArchTest
    static final ArchRule R01_error_vocabulary_knows_no_framework = noClasses().that()
        .resideInAPackage(ERROR)
        .should()
        .dependOnClassesThat(resideInAPackage("org.springframework..").or(resideInAPackage("jakarta.."))
            .or(resideInAPackage("io.autocrypt.vehicles.pki.x509..")
                .and(DescribedPredicate.not(resideInAPackage(ERROR)))))
        .as("R-01 에러 어휘(support.error)는 도메인 모델이 던지므로 프레임워크와 다른 패키지를 모른다");

    // ----- R-02 계층 의존 방향 -----

    @ArchTest
    static final ArchRule R02_usecase_does_not_know_api = noClasses().that()
        .resideInAPackage(USECASE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(API)
        .as("R-02 usecase 는 api 를 모른다");

    @ArchTest
    static final ArchRule R02_adapters_do_not_know_core = noClasses().that()
        .resideInAnyPackage(STORAGE, CLIENT)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(DOMAIN, USECASE, API)
        .as("R-02 storage 와 client 는 domain, usecase, api 를 모른다");

    // ----- R-03 이름 -----

    @ArchTest
    static final ArchRule R03_no_service_suffix = noClasses().that()
        .resideInAPackage("io.autocrypt.vehicles.pki.x509..")
        .should()
        .haveSimpleNameEndingWith("Service")
        .as("R-03 *Service 접미어는 쓰지 않는다. 도메인 서비스는 *Reader / *Writer, 흐름은 *UseCase");

    @ArchTest
    static final ArchRule R03_domain_beans_are_readers_writers_or_repositories = classes().that()
        .resideInAPackage(DOMAIN)
        .and()
        .areMetaAnnotatedWith(Component.class)
        .should()
        .haveSimpleNameEndingWith("Reader")
        .orShould()
        .haveSimpleNameEndingWith("Writer")
        .orShould()
        .haveSimpleNameEndingWith("Repository")
        .as("R-03 domain 의 Spring 빈은 *Reader, *Writer, *Repository 뿐이다");

    @ArchTest
    static final ArchRule R03_usecase_classes_are_use_cases_commands_or_results = classes().that()
        .resideInAPackage(USECASE)
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("UseCase")
        .orShould()
        .haveSimpleNameEndingWith("Command")
        .orShould()
        .haveSimpleNameEndingWith("Result")
        .as("R-03 usecase 의 클래스는 *UseCase, *Command, *Result 중 하나다");

    // ----- R-04 도메인 서비스의 범위 -----

    @ArchTest
    static final ArchRule R04_domain_services_touch_only_their_own_repository = classes().that(domainServices())
        .should(dependOnlyOnOwnRepository())
        .as("R-04 *Reader / *Writer 는 같은 이름의 *Repository 만 쓰고, 다른 *Repository, *Reader, *Writer 를 부르지 않는다");

    @ArchTest
    static final ArchRule R04_domain_services_do_not_touch_storage = noClasses().that(domainServices())
        .should()
        .dependOnClassesThat()
        .resideInAPackage(STORAGE)
        .as("R-04 *Reader / *Writer 는 storage 를 직접 쓰지 않는다. 저장은 *Repository 어댑터를 거친다");

    // ----- R-05 영속 모델은 storage 와 어댑터 안에 -----

    @ArchTest
    static final ArchRule R05_entities_live_in_storage_only = classes().that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage(STORAGE)
        .as("R-05 JPA 엔티티는 storage 패키지에만 있다");

    @ArchTest
    static final ArchRule R05_only_repository_adapters_touch_storage = noClasses().that()
        .resideOutsideOfPackages(STORAGE, SUPPORT_STORAGE)
        .and(DescribedPredicate.not(repositories()))
        .should()
        .dependOnClassesThat()
        .resideInAPackage(STORAGE)
        .as("R-05 storage(엔티티, Spring Data)는 domain 의 *Repository 어댑터만 참조한다");

    // ----- R-06 트랜잭션을 선언할 수 있는 곳 -----

    @ArchTest
    static final ArchRule R06_transactional_classes_only_in_usecase_or_domain_services = classes().that()
        .areAnnotatedWith(Transactional.class)
        .should(beTransactionBoundaryOwners())
        .allowEmptyShould(true)
        .as("R-06 @Transactional 클래스는 usecase 와 domain 의 *Reader / *Writer 에만 있다");

    @ArchTest
    static final ArchRule R06_transactional_methods_only_in_usecase_or_domain_services = methods().that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .beDeclaredInClassesThat(resideInAPackage(USECASE).or(domainServices()))
        .allowEmptyShould(true)
        .as("R-06 @Transactional 메서드는 usecase 와 domain 의 *Reader / *Writer 에만 있다");

    @ArchTest
    static final ArchRule R06_programmatic_transactions_only_in_usecase = noClasses().that()
        .resideOutsideOfPackage(USECASE)
        .should()
        .dependOnClassesThat(transactionApi())
        .as("R-06 TransactionTemplate 같은 프로그래밍 방식 트랜잭션은 usecase 에만 있다");

    // ----- R-07 도메인 서비스는 기본 전파만 쓴다 -----

    @ArchTest
    static final ArchRule R07_readers_are_read_only = classes().that(readers())
        .should(beAnnotatedWithTransactional(true))
        .as("R-07 *Reader 는 클래스에 @Transactional(readOnly = true) 를 붙인다");

    @ArchTest
    static final ArchRule R07_writers_are_read_write = classes().that(writers())
        .should(beAnnotatedWithTransactional(false))
        .as("R-07 *Writer 는 클래스에 @Transactional(readOnly = false) 를 붙인다");

    @ArchTest
    static final ArchRule R07_domain_services_use_default_propagation = methods().that()
        .areDeclaredInClassesThat(domainServices())
        .and()
        .areAnnotatedWith(Transactional.class)
        .should(useDefaultPropagation())
        .allowEmptyShould(true)
        .as("R-07 도메인 서비스는 기본 전파(REQUIRED)만 쓴다. REQUIRES_NEW 같은 경계 조절은 usecase 에서 한다");

    // ----- R-08 트랜잭션 가드 -----

    @ArchTest
    static final ArchRule R08_use_case_combining_writes_opens_a_transaction = methods().that()
        .areDeclaredInClassesThat(useCases())
        .should(openATransactionWhenCombiningWrites())
        .allowEmptyShould(true)
        .as("R-08 도메인 서비스를 둘 이상 부르고 그중 *Writer 가 있는 usecase 메서드는 @Transactional 이거나 TransactionTemplate 으로 감싼다");

    @ArchTest
    static final ArchRule R08_read_only_use_cases_do_not_write = methods().that()
        .areDeclaredInClassesThat(useCases())
        .should(notCallWritersWhenReadOnly())
        .allowEmptyShould(true)
        .as("R-08 readOnly 트랜잭션의 usecase 메서드는 *Writer 를 부르지 않는다");

    // ----- R-09 usecase 는 도메인 서비스를 거친다 -----

    @ArchTest
    static final ArchRule R09_use_cases_go_through_domain_services = noClasses().that()
        .resideInAPackage(USECASE)
        .should()
        .dependOnClassesThat(repositories().or(resideInAPackage(STORAGE)))
        .as("R-09 usecase 는 *Repository 와 storage 를 쓰지 않는다. 저장소는 *Reader / *Writer 를 거친다");

    @ArchTest
    static final ArchRule R09_use_cases_do_not_call_each_other = classes().that(useCases())
        .should(notDependOnOtherClassesNamed("UseCase"))
        .as("R-09 UseCase 는 다른 UseCase 를 호출하지 않는다");

    // ----- R-10 api 가 쓸 수 있는 것 -----

    @ArchTest
    static final ArchRule R10_api_depends_on_use_cases_and_domain_services_only = noClasses().that()
        .resideInAPackage(API)
        .should()
        .dependOnClassesThat(resideInAnyPackage(STORAGE, CLIENT).or(repositories())
            .or(resideInAPackage(DOMAIN).and(simpleNameEndingWith("Policy"))))
        .as("R-10 api 는 *Repository, *Policy, storage, client 를 쓰지 않는다. *UseCase 나 *Reader / *Writer 를 부른다");

    @ArchTest
    static final ArchRule R10_api_only_reads_aggregates = noClasses().that()
        .resideInAPackage(API)
        .should()
        .callMethodWhere(DescribedPredicate.describe("a state-changing method of a domain aggregate",
                (JavaMethodCall call) -> isAggregate(call.getTargetOwner()) && !isAccessor(call.getTarget().getName())))
        .as("R-10 api 는 돌려받은 애그리거트에서 getter 만 읽는다. 상태를 바꾸는 메서드는 부르지 않는다");

    // ----- R-11 엔드포인트 하나에 호출 하나 -----

    @ArchTest
    static final ArchRule R11_endpoint_calls_one_use_case_or_domain_service = methods().that()
        .areDeclaredInClassesThat()
        .resideInAPackage(API)
        .should(callAtMostOneEntryPoint())
        .allowEmptyShould(true)
        .as("R-11 엔드포인트 하나는 *UseCase 나 *Reader / *Writer 의 메서드를 하나만 부른다. 여러 개를 묶어야 하면 usecase 를 만든다");

    // ----- R-12 API 계약은 도메인과 분리 -----

    @ArchTest
    static final ArchRule R12_api_dtos_carry_no_domain_types = noFields().that()
        .areDeclaredInClassesThat()
        .resideInAnyPackage(API_REQUEST, API_RESPONSE)
        .should()
        .haveRawType(resideInAPackage(DOMAIN))
        .as("R-12 api 의 요청·응답 객체는 domain 타입을 필드로 갖지 않는다. 값 객체는 원시 타입이나 api 전용 record 로 푼다. enum 은 core-enum 이라 허용");

    // ----- R-13 enum 은 의존이 없다 -----

    @ArchTest
    static final ArchRule R13_enums_have_no_dependencies = noClasses().that()
        .resideInAPackage(ENUMS)
        .should()
        .dependOnClassesThat(resideInAPackage("io.autocrypt.vehicles.pki.x509..")
            .and(DescribedPredicate.not(resideInAPackage(ENUMS)))
            .or(resideInAPackage("org.springframework.."))
            .or(resideInAPackage("jakarta..")))
        .as("R-13 core-enum 의 enum 은 다른 패키지와 프레임워크에 의존하지 않는다");

    // ----- 분류 -----

    private static DescribedPredicate<JavaClass> readers() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Reader")).as("*Reader");
    }

    private static DescribedPredicate<JavaClass> writers() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Writer")).as("*Writer");
    }

    private static DescribedPredicate<JavaClass> repositories() {
        return resideInAPackage(DOMAIN).and(simpleNameEndingWith("Repository")).as("*Repository");
    }

    private static DescribedPredicate<JavaClass> domainServices() {
        return readers().or(writers()).as("domain services (*Reader, *Writer)");
    }

    private static DescribedPredicate<JavaClass> domainBeans() {
        return domainServices().or(repositories()).as("domain beans (*Reader, *Writer, *Repository)");
    }

    private static DescribedPredicate<JavaClass> domainModel() {
        return resideInAPackage(DOMAIN).and(DescribedPredicate.not(domainBeans()))
            .as("domain model (aggregates, value objects, *Policy)");
    }

    private static DescribedPredicate<JavaClass> useCases() {
        return resideInAPackage(USECASE).and(simpleNameEndingWith("UseCase")).as("*UseCase");
    }

    private static DescribedPredicate<JavaClass> transactionApi() {
        return assignableTo(TransactionOperations.class).or(assignableTo(TransactionManager.class))
            .or(assignableTo(PlatformTransactionManager.class))
            .as("programmatic transaction API");
    }

    /** 애그리거트: domain 모델 중 record, enum, *Policy 가 아닌 것. */
    private static boolean isAggregate(JavaClass javaClass) {
        return domainModel().test(javaClass) && !javaClass.isEnum() && !javaClass.isRecord()
                && !javaClass.getSimpleName().endsWith("Policy");
    }

    /** api 가 애그리거트에서 불러도 되는 메서드: 읽기 전용 접근자. */
    private static boolean isAccessor(String name) {
        return name.startsWith("get") || name.startsWith("is") || name.startsWith("has") || name.equals("toString")
                || name.equals("equals") || name.equals("hashCode") || name.endsWith("Amount");
    }

    /** 메서드의 호출. ArchUnit 은 람다 본문의 호출도 감싸는 메서드의 것으로 센다. */
    private static List<JavaMethodCall> callsOf(JavaMethod method) {
        return List.copyOf(method.getMethodCallsFromSelf());
    }

    private static Optional<Transactional> transactionalOf(JavaMethod method) {
        Optional<Transactional> onMethod = method.tryGetAnnotationOfType(Transactional.class);
        return onMethod.isPresent() ? onMethod : method.getOwner().tryGetAnnotationOfType(Transactional.class);
    }

    // ----- 조건 -----

    private static ArchCondition<JavaClass> beTransactionBoundaryOwners() {
        return new ArchCondition<>("reside in usecase or be a *Reader / *Writer") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!resideInAPackage(USECASE).test(javaClass) && !domainServices().test(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " declares @Transactional outside usecase and domain services"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnlyOnOwnRepository() {
        return new ArchCondition<>("depend only on the repository of the same aggregate") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                String aggregate = service.getSimpleName().replaceAll("(Reader|Writer)$", "");
                service.getDirectDependenciesFromSelf()
                    .stream()
                    .filter(dependency -> domainBeans().test(dependency.getTargetClass()))
                    .filter(dependency -> !dependency.getTargetClass().equals(service))
                    .filter(dependency -> !dependency.getTargetClass().getSimpleName().equals(aggregate + "Repository"))
                    .forEach(dependency -> events
                        .add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaClass> beAnnotatedWithTransactional(boolean readOnly) {
        return new ArchCondition<>("be annotated with @Transactional(readOnly = " + readOnly + ")") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<Transactional> transactional = javaClass.tryGetAnnotationOfType(Transactional.class);
                if (transactional.isEmpty() || transactional.get().readOnly() != readOnly
                        || transactional.get().propagation() != Propagation.REQUIRED) {
                    events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
                            + " must be annotated with @Transactional(readOnly = " + readOnly + ") on the class"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> useDefaultPropagation() {
        return new ArchCondition<>("use the default propagation") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Propagation propagation = method.getAnnotationOfType(Transactional.class).propagation();
                if (propagation != Propagation.REQUIRED) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " uses propagation " + propagation));
                }
            }
        };
    }

    /**
     * 람다 본문의 호출도 감싸는 메서드의 것으로 세므로, TransactionTemplate 을 쓰는 메서드에서 쓰기가 모두 템플릿 안에 있는지는 구조로
     * 판별할 수 없다. 그 부분은 리뷰(S-06)가 본다.
     */
    private static ArchCondition<JavaMethod> openATransactionWhenCombiningWrites() {
        return new ArchCondition<>("open a transaction when it combines domain service calls with a write") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> serviceCalls = callsOf(method).stream()
                    .filter(call -> domainServices().test(call.getTargetOwner()))
                    .toList();
                boolean writes = serviceCalls.stream().anyMatch(call -> writers().test(call.getTargetOwner()));
                if (serviceCalls.size() < 2 || !writes) {
                    return;
                }
                boolean declared = transactionalOf(method).filter(transactional -> !transactional.readOnly())
                    .isPresent();
                boolean programmatic = callsOf(method).stream()
                    .anyMatch(call -> transactionApi().test(call.getTargetOwner()));
                if (!declared && !programmatic) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " calls several domain services including a writer without a transaction"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notCallWritersWhenReadOnly() {
        return new ArchCondition<>("not call writers in a read-only transaction") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean readOnly = transactionalOf(method).map(Transactional::readOnly).orElse(false);
                if (!readOnly) {
                    return;
                }
                callsOf(method).stream()
                    .filter(call -> writers().test(call.getTargetOwner()))
                    .forEach(call -> events.add(SimpleConditionEvent.violated(call, call.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaMethod> callAtMostOneEntryPoint() {
        return new ArchCondition<>("call at most one use case or domain service method") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaMethodCall> calls = callsOf(method).stream()
                    .filter(call -> useCases().or(domainServices()).test(call.getTargetOwner()))
                    .toList();
                if (calls.size() > 1) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " calls " + calls.size() + " use case / domain service methods"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnOtherClassesNamed(String suffix) {
        return new ArchCondition<>("not depend on other classes ending with " + suffix) {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                origin.getDirectDependenciesFromSelf()
                    .stream()
                    .filter(dependency -> dependency.getTargetClass().getSimpleName().endsWith(suffix))
                    .filter(dependency -> !dependency.getTargetClass().equals(origin))
                    .forEach(dependency -> events
                        .add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

}
