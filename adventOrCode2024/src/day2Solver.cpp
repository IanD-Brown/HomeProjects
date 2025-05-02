
#include <vector>
#include <stdlib.h>
#include <algorithm>
#include <cassert>
#include <map>
#include <iostream>

using namespace std;

#include "day2Solver.h"
#include <iterator>

day2Solver::day2Solver(const std::string& testFile) : solver(testFile) {
    test();
    test2();
    loadFromFile();
    compute();
    compute2();
}

void day2Solver::clearData() {
    m_data.clear();
}

bool validate(int prior, int current, bool increase) {
    int diff = current - prior;
    switch (diff) {
    case -3:
    case -2:
    case -1:
        return !increase;
    case 1:
    case 2:
    case 3:
        return increase;
    }
    return false;
}

void day2Solver::loadData(const string& line) {
    m_data.push_back(asVectorInt(line, " "));
}

static bool validate1(const vector<int>& values) {
    if (values.size() > 1) {
        bool increase = values[0] < values[1];

        for (int i = 1; i < values.size(); ++i) {
            if (!validate(values[i - 1], values[i], increase)) {
                return false;
            }
        }
        return true;
    }
    return true;
}

solveResult day2Solver::compute() {
    ptrdiff_t t = count_if(m_data.begin(), m_data.end(), [](vector<int> values) { return validate1(values); });
    cout << "day2 " << t << endl;

    return t;
}

static bool validate2(const vector<int>& values) {
    if (!validate1(values)) {
        for (int i = 0; i < values.size(); ++i) {
            vector<int> v2;
            for (int j = 0; j < values.size(); ++j) {
                if (j != i) {
                    v2.push_back(values[j]);
                }
            }
            if (validate1(v2)) {
//                cout << "Validated2 ";
//                std::copy(values.begin(), values.end(), std::ostream_iterator<int>(std::cout, " "));
//                cout << endl;
                return true;
            }
        }
        return false;
    }
//    cout << "Validated1 ";
//    std::copy(values.begin(), values.end(), std::ostream_iterator<int>(std::cout, " "));
//    cout << endl;
    return true;
}

solveResult day2Solver::compute2() {
    ptrdiff_t t = count_if(m_data.begin(), m_data.end(), [](vector<int> values) { return validate2(values); });
    cout << "day2b " << t << endl;

    return t;
}

void day2Solver::test() {
    loadTestData();
    ptrdiff_t t  = compute();

    assert(t == 2 && "distance count should match");
    clearData();
}

void day2Solver::loadTestData()
{
    loadData("7 6 4 2 1");
    loadData("1 2 7 8 9");
    loadData("9 7 6 2 1");
    loadData("1 3 2 4 5");
    loadData("8 6 4 4 1");
    loadData("1 3 6 7 9");
}

void day2Solver::test2() {
    loadTestData();
    ptrdiff_t t = compute2();

    assert(t == 4 && "test2 count should match");
    clearData();
}
