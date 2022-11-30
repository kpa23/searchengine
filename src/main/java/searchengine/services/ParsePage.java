package searchengine.services;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import searchengine.config.Parse;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;
import searchengine.repository.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;


public class ParsePage extends RecursiveTask<List<String>> {
    private static final Logger logger = LogManager.getLogger(ParsePage.class);

    private final String url;
    private final String domain;
    private ParsePage parent;
    private final List<ParsePage> links;
    private int level;
    private final SiteT siteT;
    private final PageTRepository pageTRepository;
    private final SiteTRepository siteTRepository;
    private final Parse parse;
    private final ConcurrentHashMap<String, ParsePage> uniqueLinks;
    private Connection.Response response = null;

    public Connection.Response getResponse() {
        return response;
    }

    public ParsePage(String url, ParsePage parent) {
        this(url, parent.domain, parent.siteT, parent.pageTRepository, parent.siteTRepository, parent.parse, parent.uniqueLinks);
        this.parent = parent;
        this.level = parent.level + 1;
    }

    public ParsePage(String url, String domain, SiteT siteT
            , PageTRepository pageTRepository, SiteTRepository siteTRepository, Parse parse
            , ConcurrentHashMap<String, ParsePage> uniqueLinks) {
        this.url = url;
        this.domain = domain;
        this.parent = null;
        this.links = new ArrayList<>();
        this.level = 0;
        this.siteT = siteT;
        this.pageTRepository = pageTRepository;
        this.siteTRepository = siteTRepository;
        this.parse = parse;
        this.uniqueLinks = uniqueLinks;
    }

    @Override
    protected List<String> compute() {
        List<String> list = new ArrayList<>();
        List<ParsePage> tasks = new ArrayList<>();

        Document document;
        uniqueLinks.put("/", this);

        try {
            document = downloadAndSavePage();
            savePage(document.body().text());
            TimeUnit.MILLISECONDS.sleep((int) (Math.random() * 100));
            document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            Elements elements = document.select("a[href~=^/?([\\w\\d/-]+)?]");
            parseAllHrefs(elements, list, tasks);
            addResultsFromTasks(list, tasks);
        } catch (IOException | InterruptedException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            savePage("");
        }

        return list;
    }

    public Document downloadAndSavePage() throws IOException {
        Document document;
        try {
            response = Jsoup.connect(domain + url)
                    .userAgent(parse.getUseragent())
                    .referrer("https://www.google.com")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();

            if (response.statusCode() != 200) throw new IOException(String.valueOf(response.statusCode()));
            document = response.parse();

        } catch (IOException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
            savePage("");
            return null;
        }
        return document;

    }

    private void parseAllHrefs(Elements elements, List<String> list, List<ParsePage> tasks) {
        for (Element e : elements) {
            String href = e.attr("href").replace("//www.", "//");
            if (!href.startsWith(this.domain) &&
                    (href.startsWith("http") || href.isEmpty() || href.contains("#") || href.contains(":")))
                continue;
            String checkUrl = generateUrl(href).replace(domain, "");
            if (!checkAddUrl(checkUrl)) {
                list.add(checkUrl);
                ParsePage newParse = new ParsePage(checkUrl, this);
                newParse.fork();
                tasks.add(newParse);
                links.add(newParse);
            }
        }
    }

    public PageT savePage(String text) {
        PageT p = new PageT(siteT.getSiteId(), url, response.statusCode(), text);
        pageTRepository.save(p);
        siteT.setStatusTime(Utils.getTimeStamp());
        siteTRepository.save(siteT);
        return p;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t".repeat(this.level)).append(this.url).append("\n");
        this.links.forEach(e -> sb.append(e.toString()));

        return sb.toString();
    }

    private void addResultsFromTasks(List<String> list, List<ParsePage> tasks) {
        for (ParsePage item : tasks) {
            list.addAll(item.join());
        }
    }

    private boolean checkAddUrl(String url) {
        boolean isExist = uniqueLinks.containsKey(url);
        if (!isExist) {
            uniqueLinks.put(url, this);
        }
        return isExist;
    }

    private String generateUrl(String href) {
        String result;
        if (!href.startsWith(this.domain)) {
            if (!href.startsWith("/")) {
                if (!url.endsWith("/")) {
                    if (this.domain.equals(url)) {
                        result = url + "/" + href;
                    } else {
                        result = url.substring(0, url.lastIndexOf("/") + 1) + href;
                    }
                } else {
                    result = url + href;
                }
            } else {
                result = this.domain + href;
            }
        } else {
            result = href;
        }
        return result;
    }
}
