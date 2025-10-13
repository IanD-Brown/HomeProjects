
#include "day3Solver.h"
#include <regex>
#include <solver.h>
#include <string>

using namespace std;

day3Solver::day3Solver(const string& testFile) : solver(testFile) {
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
        }
    }

    return t;
}

void day3Solver::loadTestData() {
    clearData();
    loadData("xmul(2,4)&mul[3,7]!^don't()_mul(5,5)+mul(32,64](mul(11,8)undo()?mul(8,5))");
}
