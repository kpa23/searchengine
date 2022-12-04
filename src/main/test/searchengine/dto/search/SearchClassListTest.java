package searchengine.dto.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import searchengine.model.IndexT;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchClassListTest {

    private List<IndexT> indexTList;
    private List<IndexT> indexTList2;
    private SearchClassList searchClassList;
    private SearchClassList searchClassList2;
    private List<SearchClassData> searchClassData;

    @BeforeEach
    void setUp() {
        searchClassList = new SearchClassList("search");
        searchClassList2 = new SearchClassList("search");
        indexTList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            indexTList.add(new IndexT(i, 1, i));
        }
        indexTList2 = new ArrayList<>();
        for (int i = 3; i < 9; i++) {
            indexTList2.add(new IndexT(i, 1, i));
        }
        searchClassData = new ArrayList<>();
        for (int i = 7; i < 15; i++) {
            searchClassData.add(new SearchClassData(i, i));
        }
        searchClassList.addList(indexTList);
        searchClassList2.addList(indexTList2);
    }

    @Test
    void appendList() {
        assertEquals(5, searchClassList.getData().size());
        assertEquals(6, searchClassList2.getData().size());
        searchClassList.addList(indexTList2);
        assertEquals(11, searchClassList.getData().size());
    }
    @Test
    void Intersect() {
        searchClassList.intersectData(searchClassList2.getData());
        assertEquals(2, searchClassList.getData().size());
    }
    @Test
    void Intersect2() {
        searchClassList.addList(indexTList2);
        searchClassList.intersectData(searchClassList2.getData());
        assertEquals(8, searchClassList.getData().size());

    }
}