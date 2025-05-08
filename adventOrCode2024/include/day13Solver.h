#pragma once
#include <map>
#include <set>

#include "solver.h"

class day13Solver : public solver {
private:
	friend struct MachineBuilder;
	struct Machine {
		const size_t m_aX;
		const size_t m_aY;
		const size_t m_bX;
		const size_t m_bY;
		const size_t m_prizeX;
		const size_t m_prizeY;

		Machine(size_t aX, size_t aY, size_t bX, size_t bY, size_t prizeX, size_t prizeY) :
			m_prizeX(prizeX),
			m_prizeY(prizeY),
			m_aX(aX),
			m_aY(aY),
			m_bX(bX),
			m_bY(bY) {}

		bool getsThePrize(size_t aMultiple, size_t bMultipl) const {
			return m_aX * aMultiple + m_bX * bMultipl == m_prizeX &&
				   m_aY * aMultiple + m_bY * bMultipl == m_prizeY;
		}

		solveResult cost(solveResult prizeShift) const {
			const solveResult aX = m_aX;
			const solveResult aY = m_aY;
			const solveResult bX = m_bX;
			const solveResult bY = m_bY;
			const solveResult pX = m_prizeX + prizeShift;
			const solveResult pY = m_prizeY + prizeShift;
			const solveResult denominator = aX * bY - aY * bX;
			const solveResult a = (pX * bY - pY * bX) / denominator;
			const solveResult b = (aX * pY - aY * pX) / denominator;

			return (aX * a + bX * b == pX && aY * a + bY * b == pY) ? a * 3LL + b : 0;
		}

		std::string toString() const {
			return "aX " + std::to_string(m_aX) + " bX " + std::to_string(m_bX) + " aY " + std::to_string(m_aY) + " bY " +
				   std::to_string(m_bY) + " prizeX " + std::to_string(m_prizeX) + " prizeY " + std::to_string(m_prizeY);
		}
	};
	std::vector<Machine> m_data;

public:
	day13Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};

