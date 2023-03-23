package study.querydsl;


import com.querydsl.core.QueryResults;
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




}
