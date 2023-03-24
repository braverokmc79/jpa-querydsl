package study.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static study.querydsl.entity.QMember.*;
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
    

}
