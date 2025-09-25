#include <cassert>
#include <iostream>
#include <vector>

#include "day4Solver.h"

using namespace std;

static const string c_search = "XMAS";

struct SearchLocation {
    int m_r;
    int m_c;
    SearchLocation(int r, int c) : m_r(r), m_c(c) {}
};

struct SearchPoint {
    int m_rowOffset;
    int m_colOffset;
    char m_target;
    SearchPoint(int rowOffset, int colOffset, char target) : m_rowOffset(rowOffset), m_colOffset(colOffset), m_target(target) {}
};

struct SearchPath {
    vector <SearchLocation> m_searchLocations;

    SearchPath(int r1, int c1, int r2, int c2, int r3, int c3) {
        m_searchLocations.push_back(SearchLocation(r1, c1));
        m_searchLocations.push_back(SearchLocation(r2, c2));
        m_searchLocations.push_back(SearchLocation(r3, c3));
    }

    bool isMatch(const vector<string> data, int row, int col) {
        const size_t rowLimit = data.size();
        const size_t colLimit = data[0].size();

        for (int i = 0; i < m_searchLocations.size(); ++i) {
            const SearchLocation& searchLocation = m_searchLocations[i];
            int r = row + searchLocation.m_r;
            int c = col + searchLocation.m_c;

            if (r < 0 || r >= rowLimit || c < 0 || c >= colLimit || data[r][c] != c_search[i + 1]) {
                return false;
            }
        }
        return true;
    }
};

day4Solver::day4Solver(const string& testFile) : solver(testFile) {
    test();
    test2();
    loadFromFile();
    compute();
    compute2();
}

void day4Solver::loadData(const string& line) {
    if (!m_data.empty()) {
        assert(m_data[0].size() == line.size());
    }
    m_data.push_back(line);
}

void day4Solver::clearData() {
    m_data.clear();
}

solveResult day4Solver::compute() {
	solveResult t = 0;
    vector<SearchPath> searchPaths;

    // same row forwards and backwards
    searchPaths.push_back({0, 1, 0, 2, 0, 3});
    searchPaths.push_back({ 0, -1, 0, -2, 0, -3 });
    // same row up and down
    searchPaths.push_back({ 1, 0, 2, 0, 3, 0 });
    searchPaths.push_back({ -1, 0, -2, 0, -3, 0 });
    // diagonal
    searchPaths.push_back({ 1, 1, 2, 2, 3, 3 });
    searchPaths.push_back({ -1, -1, -2, -2, -3, -3 });
    searchPaths.push_back({ 1, -1, 2, -2, 3, -3 });
    searchPaths.push_back({ -1, 1, -2, 2, -3, 3 });

    for (int r = 0; r < m_data.size(); ++r) {
        const string& row = m_data[r];
        for (int c = 0; c < row.size(); ++c) {
            if (row[c] == c_search[0]) {
                for (auto sp : searchPaths) {
                    if (sp.isMatch(m_data, r, c)) {
                        ++t;
                    }
                }
            }
        }
    }

    return t;
}

static bool isMatch(const vector<string>& data, int r, int c, const vector<SearchPoint>& search) {
    for (int i = 0; i < search.size(); ++i) {
        const SearchPoint& p = search[i];
        if (data[r + p.m_rowOffset][c + p.m_colOffset] != p.m_target) {
            return false;
        }
    }
    return true;
}

solveResult day4Solver::compute2() {
    long t = 0;
    vector<SearchPoint> searching1;
    vector<SearchPoint> searching2;
    vector<SearchPoint> searching3;
    vector<SearchPoint> searching4;

    searching1.push_back({ 0, 0, 'M' });
    searching1.push_back({ 0, 2, 'S' });
    searching1.push_back({ 1, 1, 'A' });
    searching1.push_back({ 2, 0, 'M' });
    searching1.push_back({ 2, 2, 'S' });

    searching2.push_back({ 0, 0, 'S' });
    searching2.push_back({ 0, 2, 'M' });
    searching2.push_back({ 1, 1, 'A' });
    searching2.push_back({ 2, 0, 'S' });
    searching2.push_back({ 2, 2, 'M' });

    searching3.push_back({ 0, 0, 'S' });
    searching3.push_back({ 0, 2, 'S' });
    searching3.push_back({ 1, 1, 'A' });
    searching3.push_back({ 2, 0, 'M' });
    searching3.push_back({ 2, 2, 'M' });

    searching4.push_back({ 0, 0, 'M' });
    searching4.push_back({ 0, 2, 'M' });
    searching4.push_back({ 1, 1, 'A' });
    searching4.push_back({ 2, 0, 'S' });
    searching4.push_back({ 2, 2, 'S' });

    for (int r = 0; r < m_data.size() - 2; ++r) {
        for (int c = 0; c < m_data[0].size() - 2; ++c) {
            if (isMatch(m_data, r, c, searching1) || isMatch(m_data, r, c, searching2) || isMatch(m_data, r, c, searching3) || isMatch(m_data, r, c, searching4)) {
                ++t;
            }
        }
    }

    return t;
}

void day4Solver::test() {
    loadTestData();
	solveResult t = compute();

    assert(t == 18 && "distance count should match");
    clearData();
}

void day4Solver::loadTestData() {
    loadData("MMMSXXMASM");
    loadData("MSAMXMSMSA");
    loadData("AMXSXMAAMM");
    loadData("MSAMASMSMX");
    loadData("XMASAMXAMM");
    loadData("XXAMMXXAMA");
    loadData("SMSMSASXSS");
    loadData("SAXAMASAAA");
    loadData("MAMMMXMMMM");
    loadData("MXMXAXMASX");
}

void day4Solver::test2() {
    loadTestData();
	solveResult t = compute2();

    assert(t == 9 && "test2 count should match");
    clearData();
}
