package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
//@Rollback(value = false)
@Commit
class ApplicationTests {

	@Autowired
	EntityManager em;

	@Autowired
	JPAQueryFactory jpaQueryFactory;

	@Test
	void contextLoads() {
		Hello hello=new Hello();
		em.persist(hello);

		JPAQueryFactory query=new JPAQueryFactory(em);
		//QHello qHello=new QHello("h");
		QHello qHello=QHello.hello;  //Querydsl Q타입 동작 확인
		Hello result =query.selectFrom(qHello).fetchOne();


		Assertions.assertThat(result).isEqualTo(hello);
		//lombok 동작 확인(hello.getId());
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
	}


	@Test
	void contextLoads2() {
		Hello hello=new Hello();
		em.persist(hello);


		QHello qHello=QHello.hello;
		Hello result =jpaQueryFactory.selectFrom(qHello).fetchOne();

		Assertions.assertThat(result).isEqualTo(hello);
		//lombok 동작 확인(hello.getId());
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
	}

}
