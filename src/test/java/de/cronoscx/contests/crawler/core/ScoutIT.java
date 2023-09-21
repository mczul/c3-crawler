package de.cronoscx.contests.crawler.core;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.cronoscx.contests.crawler.core.Scout.Report;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@WireMockTest
public class ScoutIT {

    private final Memory memory = new Memory();
    private final Scout underTest = new Scout(memory);

    @Nested
    class Check {

        @Test
        void should_find_and_resolve_links_in_document(WireMockRuntimeInfo wireMockRuntimeInfo)
            throws URISyntaxException, IOException {

            // given
            ClassPathResource res = new ClassPathResource("example-page.html");
            String url = wireMockRuntimeInfo.getHttpBaseUrl();
            stubFor(get("/").willReturn(
                ok(res.getContentAsString(StandardCharsets.UTF_8)).withHeader("content-type", "text/html")));

            // when
            Report report = underTest.check(new URI(url), "foobar");

            // then
            assertThat(report.found()).isFalse();
            assertThat(report.references()).containsExactly(//
                new URI("http://www.cronos.de"), //
                new URI("https://twitter.com/CronosGmbh"), //
                new URI("https://www.cronoscx.de/"), //
                new URI("https://www.cronoscx.de/blog"), //
                new URI("https://www.cronoscx.de/custom-code"), //
                new URI("https://www.cronoscx.de/datenschutz"), //
                new URI("https://www.cronoscx.de/emarsys"), //
                new URI("https://www.cronoscx.de/home"), //
                new URI("https://www.cronoscx.de/impressum"), //
                new URI("https://www.cronoscx.de/jobs"), //
                new URI("https://www.cronoscx.de/jobscontent/java-fullstack-entwickler-d-m-w"), //
                new URI("https://www.cronoscx.de/jobscontent/junior-softwareentwickler-d-m-w-in-cottbus"), //
                new URI("https://www.cronoscx.de/leistungen"), //
                new URI("https://www.cronoscx.de/login"), //
                new URI("https://www.cronoscx.de/sapsalescloud"), //
                new URI("https://www.cronoscx.de/sapservicecloud"), //
                new URI("https://www.cronoscx.de/success-storys"), //
                new URI(
                    "https://www.cronoscx.de/successcontent/dvv-dessau-vertrieb-mit-der-sap-sales-und-service-cloud"),
                //
                new URI(
                    "https://www.ey.com/de_de/news/2023/06/energiekrise-beschleunigt-transformation-stadtwerke-reagieren-mit-neuen-strategien"),
                //
                new URI("https://www.facebook.com/cronosgmbh/"), //
                new URI("https://www.instagram.com/cronologen/"), //
                new URI("https://www.linkedin.com/company/cronosgmbh/"), //
                new URI("https://www.onetoone.de/artikel/db/078774gehl.html"), //
                new URI("https://www.xing.com/companies/cronosunternehmensberatunggmbh"), //
                new URI("https://www.youtube.com/channel/UCLv5p8SP4PM-ZOAdFkyPwzg") //
            );

        }
    }

}
