
#include "day2Solver.h"
#include <algorithm>
#include <solver.h>
#include <string>
#include <vector>

using namespace std;

day2Solver::day2Solver(const std::string& testFile) : solver(testFile) {
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
                return true;
            }
        }
        return false;
    }
    return true;
}

solveResult day2Solver::compute2() {
    ptrdiff_t t = count_if(m_data.begin(), m_data.end(), [](vector<int> values) { return validate2(values); });

    return t;
}

void day2Solver::loadTestData() {
    clearData();
    loadData("7 6 4 2 1");
    loadData("1 2 7 8 9");
    loadData("9 7 6 2 1");
    loadData("1 3 2 4 5");
    loadData("8 6 4 4 1");
    loadData("1 3 6 7 9");
}
