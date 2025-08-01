
#include <vector>
#include <cstdlib>
#include <algorithm>
#include <cassert>
#include <map>
#include <iostream>
#include <iterator>
#include <regex>

using namespace std;

#include "day3Solver.h"

day3Solver::day3Solver(const string& testFile) : solver(testFile) {
    test();
    test2();
    loadFromFile();
    compute();
	solveResult v = compute2();
	solveResult diff = v - 74838033;

    assert(v == 74838033 && " not as expected");
}

void day3Solver::clearData() {
    m_data = "";
}

void day3Solver::loadData(const string& line) {
    m_data += line + " ";
}

static long performMul(const string& str) {
    size_t first = str.find("(");
    size_t second = str.find(",");
    string a = str.substr(first + 1, second - (first + 1));
    string b = str.substr(second + 1);
//    cout << a << "*" << b << endl;
    return stoi(a) * stoi(b);
}

solveResult day3Solver::compute() {
    long t = 0;
    const regex pattern("mul\\(\\d+,\\d+\\)");
    smatch matches;

    for (sregex_iterator i = sregex_iterator(m_data.begin(), m_data.end(), pattern); i != sregex_iterator(); ++i) {
        smatch match = *i;
        t += performMul(match[0]);
    }
    cout << "day3 " << t << endl;

    return t;
}

solveResult day3Solver::compute2() {
    long t = 0;
    const regex pattern("don't\\(\\)|do\\(\\)|mul\\(\\d{1,3},\\d{1,3}\\)");
    smatch matches;
    bool adding = true;

    regex_token_iterator<string::iterator> rend;

    regex_token_iterator<string::iterator> a(m_data.begin(), m_data.end(), pattern);
    while (a != rend) {
        string matched = *a++;
        if (matched == "do()") {
            adding = true;
        } else if (matched == "don't()") {
            adding = false;
        } else if (adding) {
            t += performMul(matched);
        } else {
//            cout << "not adding " << matched << endl;
        }
    }

    cout << "day3b " << t << endl;

    return t;
}

void day3Solver::test() {
    loadData("xmul(2,4)%&mul[3,7]!@^do_not_mul(5,5)+mul(32,64]then(mul(11,8)mul(8,5))");
	solveResult t  = compute();

    assert(t == 161 && " should match");
    clearData();
}

void day3Solver::test2() {
    loadData("xmul(2,4)&mul[3,7]!^don't()_mul(5,5)+mul(32,64](mul(11,8)undo()?mul(8,5))");
	solveResult t = compute2();

    assert(t == 48 && "test2 count should match");
    clearData();
}
