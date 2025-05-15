
#include <vector>
#include <stdlib.h>
#include <algorithm>
#include <cassert>
#include <map>

using namespace std;

#include "day1Solver.h"

day1Solver::day1Solver(const std::string& testFile) : solver(testFile) {
    test();
    test2();
    loadFromFile();
    compute();
    compute2();
}

void day1Solver::clearData() {
    m_left.clear();
    m_right.clear();
}

void day1Solver::loadData(const string& line) {
    size_t pos = line.find("   ");
    m_left.push_back(stoi(line.substr(0, pos)));
    m_right.push_back(stoi(line.substr(pos)));
}

solveResult day1Solver::compute() {
    sort(m_left.begin(), m_left.end());
    sort(m_right.begin(), m_right.end());
    int t = 0;
    for (int i = 0; i < m_left.size(); ++i) {
        t += abs(m_right.at(i) - m_left.at(i));
    }

    fprintf(stdout, "computed % d\n", t);

    return t;
}

solveResult day1Solver::compute2() {
    map<int, int> counts;
    int t = 0;
    for (int r : m_right) {
        if (counts.find(r) == counts.end()) {
            counts[r] = 1;
        } else {
            counts[r] += 1;
        }
    }
    for (int l : m_left) {
        if (counts.find(l) != counts.end()) {
            t += l * counts[l];
        }
    }

    fprintf(stdout, "computed2 % d\n", t);

    return t;
}

void day1Solver::test() {
    loadTestData();
	solveResult t  = compute();

    assert(t == 11 && "distance count should match");
    clearData();
}

void day1Solver::loadTestData()
{
    loadData("3   4");
    loadData("4   3");
    loadData("2   5");
    loadData("1   3");
    loadData("3   9");
    loadData("3   3");
}

void day1Solver::test2() {
    loadTestData();
	solveResult t = compute2();

    assert(t == 31 && "test2 count should match");
    clearData();
}
