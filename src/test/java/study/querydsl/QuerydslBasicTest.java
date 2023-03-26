package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.*;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.xmlunit.builder.Input.from;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before(){
        queryFactory=new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB=new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1=new Member("member1", 10, teamA);
        Member member2=new Member("member2", 20, teamA);
        Member member3=new Member("member3", 30, teamB);
        Member member4=new Member("member4", 40, teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){

        String qlString="" +
                "select m from Member m   " +
                " where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1").getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }



    @Test
    public void startQuerydsl(){
        //member1을 찾아라.

//        QMember m=new QMember("m");

        Member findMember=queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))  //파라미터 바인딩 철
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search(){
        List<Member> findMembers = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))
                ).fetch();

        Assertions.assertThat(findMembers.get(0).getUsername()).isEqualTo("member1");
    }


    @Test
    public void search2(){
        List<Member> findMembers = queryFactory.selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30))
                ).fetch();

        Assertions.assertThat(findMembers.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        List<Member> findMembers = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                ).fetch();

        Assertions.assertThat(findMembers.get(0).getUsername()).isEqualTo("member1");
    }


    @Test
    public void resultFetch(){
        //List
        List<Member> fetch = queryFactory
                                .selectFrom(member)
                                .fetch();


        //단건
        Member findMember1 = queryFactory
                                .selectFrom(member)
                                .fetchOne();


        //처음 한 건 조회
        Member findMember2=queryFactory
                            .selectFrom(member)
                            .fetchFirst();


        //페이징에서 사용
        QueryResults<Member> results =queryFactory
                    .selectFrom(member).fetchResults();

            results.getTotal();
            List<Member> content=results.getResults();


            //count 쿼리로 사용
        long count=queryFactory
                .selectFrom(member)
                .fetchCount();

    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public  void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();


        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }



    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
    }


    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member).fetch();

        Tuple tuple =result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     * @throws Exception
     */
    @Test
    public void group() throws Exception{

        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member).join(member.team, team)
                .groupBy(team.name).fetch();

        Tuple teamA=result.get(0);
        Tuple teamB=result.get(1);

       Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
       Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15); //(10+20)/2

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(35); //(30+40)/2

    }


    /**
     * 팀 A 에 소속된 모든 회원
     */

    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join (member.team,team)
                //.leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }


    /**
     * 연관 관계가 없는 것도 조인이 가능하다.
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */

    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        /** 기존과 다르게 from 절에서 두개 테이블을 묶어줬다.
         * 모든 팀테이블 조인 세타 조인  =>  member member0_ cross
         *
         selec member1  from
         Member member1,
                Team team
         where
            member1.username = team.name
         * **/
        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name)).fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
        
    }


    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모드 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                //.leftJoin(member.team, team)
                .join(member.team, team)
                //.on(team.name.eq("teamA"))
                //inner join  경우 같다 on 절을 쓰나 where 절을 쓰나 동일하다.
                .where(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple :result){
            System.out.println("tuple = " + tuple);
        }

        /** innerjoin 경우 다음 방법 을 추천 **/
        queryFactory
                .select(member,team)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
    }


    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamB"));


        //주의  .leftJoin(team) 엔티티 하나만 들어갔다.
        List<Tuple> result = queryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();


        for(Tuple tuple :result){
            System.out.println("tuple = " + tuple);
        }
    }
//    =>출력
    /**
     *

    select
        member1,
        team
    from
        Member member1
    left join
        Team team with member1.username = team.name

     // select
    member0_.member_id as member_i1_1_0_,
    team1_.team_id as team_id1_2_1_,
    member0_.age as age2_1_0_,
    member0_.team_id as team_id4_1_0_,
    member0_.username as username3_1_0_,
    team1_.name as name2_2_1_
            from
    member member0_
    left outer join
    team team1_
    on (
            member0_.username=team1_.name
    )
     */

//    tuple = [Member(id=3, username=member1, age=10), null]
//    tuple = [Member(id=4, username=member2, age=20), null]
//    tuple = [Member(id=5, username=member3, age=30), null]
//    tuple = [Member(id=6, username=member4, age=40), null]
//    tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
//    tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
//    tuple = [Member(id=9, username=teamB, age=0), Team(id=2, name=teamB)]



    @PersistenceUnit
    EntityManagerFactory emf;


    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member finndMember =queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

     boolean loaded=emf.getPersistenceUnitUtil().isLoaded(finndMember.getTeam());
     Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
    }


    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member finndMember =queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded=emf.getPersistenceUnitUtil().isLoaded(finndMember.getTeam());
        Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
    }


    /**
     * 나이가 가장 많은 회원 조회
     */

    @Test
    public void subQuery(){
        QMember memberSub =new QMember("memberSub");

        List<Member> reseult = queryFactory.
                selectFrom(member)
                .where(member.age.eq(
                                select(memberSub.age.max())
                                        .from(memberSub)
                        )
                ).fetch();


        Assertions.assertThat(reseult).extracting("age")
                .containsExactly(40);
    }




    /**
     * 나이가 평균 이상인회원
     * lt <
     *
     * loe <=
     *
     * gt >
     *
     * goe >=
     */

    @Test
    public void subQueryGoe(){
        QMember memberSub =new QMember("memberSub");

        List<Member> reseult = queryFactory.
                selectFrom(member)
                .where(member.age.goe(
                                select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                ).fetch();


        Assertions.assertThat(reseult).extracting("age")
                .containsExactly(30,40);
    }


    @Test
    public void subQueryIn(){
        QMember memberSub =new QMember("memberSub");

        List<Member> reseult = queryFactory.
                selectFrom(member)
                .where(member.age.in(
                                select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        )
                ).fetch();


        Assertions.assertThat(reseult).extracting("age")
                .containsExactly(20,30,40);
    }


    /**
     * 셀렉트 절에서 서브쿼리 사용
     */
    @Test
    public void selectSubQuery(){
        QMember memberSub =new QMember("memberSub");
        
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                        .from(memberSub))
                .from(member)
                .fetch();

        for(Tuple tuple: result){
            System.out.println("tuple = " + tuple);
        }
    }


    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for(String s :result){
            System.out.println("s = " + s);
        }
    }

    @Test
    public void  complexCase(){
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21-~30살")
                                .otherwise("기탕")
                )
                .from(member)
                .fetch();

        for(String s :result){
            System.out.println("s = " + s);
        }

    }


    @Test
    public  void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    @Test
    public void concat(){
        //{username}_{age}
        List<String> result = queryFactory.select(
                        member.username.concat("_")
                                .concat(member.age.stringValue())
                )
                .from(member)
                .fetch();
        for (String s :result){
            System.out.println("s = " + s);
        }

    }



    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }


    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory.
                select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

    }



    @Test
    public void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }



    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /** 
     * 생성자 방식
     * **/
    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }



    @Test
    public void findUserDto(){
        QMember memberSub =new QMember("memberSub");

        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"),
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")
                        )
                ).from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }



    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }



    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam ="member1";
        Integer ageParam =null;

        List<Member> result=searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        /** username 이 필수값이아여야 할경우 다음과 같이 BooleanBuilder 생성자 설정
        BooleanBuilder builder =new BooleanBuilder(member.username.eq(usernameCond));
        */
         BooleanBuilder builder =new BooleanBuilder(member.username.eq(usernameCond));
        if(usernameCond!=null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond!=null){
            builder.and(member.age.eq(ageCond));
        }
        return  queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }



    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam ="member1";
        Integer ageParam =null;

        List<Member> result=searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }


    private List<Member> searchMember2(String usernameCond, Integer ageCond){

       //** where 조건에서 null 이면 무시한다.
        return queryFactory
                .selectFrom(member)
               // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
   }

    private BooleanExpression usernameEq(String usernameCond) {
       return  usernameCond!=null? member.username.eq(usernameCond) : null ;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond!=null ? member.age.eq(ageCond) : null;
    }


    /**
     *  다음과 같이 조합할 수 있다.
     */
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    @Test
    @Commit
    public void bulkUpdate(){
        //member1 =10 -> DB member1
        //member2 =20 -> DB member2
        //member3 =30 -> DB member3
        //member4 =40 -> DB member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();
        //member1 =10 -> DB 비회원
        //member2 =20 -> DB 비회원
        //member3 =30 -> DB member3
        //member4 =40 -> DB member4

        List<Member> result = queryFactory.selectFrom(member).fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }


    /**
     * 벌크연산 더하기
     */
    @Test
    public void bulkAdd(){
        long count =queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    /**
     * 벌크 연산 곱하기
     */
    @Test
    public void bulkMultiple(){
        long count =queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }


    /**
     * 벌크 연산 삭제
     */
    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }





}
